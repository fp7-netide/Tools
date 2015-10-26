/*
 *  Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 *  Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 *  Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 *  Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 *  Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.gc;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.util.KryoNamespace;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationListener;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import eu.netide.gc.store.DefaultMMSEntry;
import eu.netide.gc.store.MMSStoreEntry;

/**
 * Skeletal ONOS application component.
 */
@Service
@Component(immediate = true)
public class GCManager implements GCServices {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService appAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private final InternalAppListener myAppListener = new InternalAppListener();
    private final FlowRuleListener flowListener = new InternalFlowListener();
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private EventuallyConsistentMap<DeviceId, Set<MMSStoreEntry>> mmsDB = null;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Application application = null;
    private Set<FlowRule> uninstallRules = Sets.newHashSet();
    private Set<FlowRule> newRules = Sets.newHashSet();

    private final Timer timer = new Timer("garbage-collector-timer");
    private TimerTask deleteAppRulesTask = null;
    private static final long GC_TIMEOUT = 5000;

    private ApplicationId appId;

    @Activate
    protected void activate() {

        appId = coreService.registerApplication("eu.netide.gc");
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX);
        requestPackets();
        appAdminService.addListener(myAppListener);
        flowRuleService.addListener(flowListener);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder().register(KryoNamespaces.API)
                .register(MMSStoreEntry.class);

        EventuallyConsistentMapBuilder<DeviceId, Set<MMSStoreEntry>> mmsDBBuilder = storageService
                .<DeviceId, Set<MMSStoreEntry>>eventuallyConsistentMapBuilder();

        mmsDB = mmsDBBuilder.withName("mmsstore").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    protected void deactivate() {
        appAdminService.removeListener(myAppListener);
        flowRuleService.removeListener(flowListener);
        packetService.removeProcessor(processor);
        mmsDB.destroy();
        processor = null;
        log.info("Stopped");
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestPackets() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private synchronized void startGCTimer() {
        stopGCTimer();
        deleteAppRulesTask = new DeleteRuleTask();
        timer.schedule(deleteAppRulesTask, GC_TIMEOUT);
    }

    private synchronized void stopGCTimer() {
        if (deleteAppRulesTask != null) {
            deleteAppRulesTask.cancel();
            deleteAppRulesTask = null;
        }

    }

    private class DeleteRuleTask extends TimerTask {

        @Override
        public void run() {
            try {
                for (FlowRule f : newRules) {
                    if (uninstallRules.contains(f)) {
                        uninstallRules.remove(f);
                    }
                }

                if (!uninstallRules.isEmpty()) {

                    log.info("Rules that should be deleted: {}", uninstallRules);
                    flowRuleService.removeFlowRules(Iterables.toArray(uninstallRules, FlowRule.class));

                } else {
                    log.info("There are no Flow Mod to delete!");
                }

                // clear all the support variables
                application = null;
                uninstallRules.clear();
                newRules.clear();

            } catch (Exception e) {
                log.warn("Unable to handle garbage collector request due to {}", e.getMessage());
                log.warn("Boom!", e);
            }
        }

    }

    private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {

            FlowRule f = event.subject();

            if (event.type() == FlowRuleEvent.Type.RULE_REMOVED) {
                MMSStoreEntry flowEntry = new DefaultMMSEntry(f);
                if (mmsDB.containsKey(f.deviceId())) {

                    Set<MMSStoreEntry> entries = mmsDB.get(f.deviceId());
                    if (entries.contains(flowEntry)) {
                        entries.remove(flowEntry);
                        mmsDB.put(flowEntry.deviceId(), entries);
                    }

                }
            }

            // log.debug("Event type: {}, Application: {}", event.type(),
            // event.subject().appId());

            if (application != null) {
                if ((event.type() == FlowRuleEvent.Type.RULE_ADD_REQUESTED) && (application.id().id() == f.appId())) {
                    if (f.isPermanent()) {
                        log.info("Saving new flow mods for app {}", application.id().name());
                        newRules.add(f);
                    }

                }
            }

        }
    }

    private class InternalAppListener implements ApplicationListener {

        @Override
        public void event(ApplicationEvent event) {
            // App needs to be restarted
            if (event.type() == ApplicationEvent.Type.APP_DEACTIVATED) {
                // if (appAdminService.getState(event.subject().id()) ==
                // ApplicationState.INSTALLED) {

                log.info("App {} deactived, Garbage Collector is starting...", event.subject().id().name());
                flowRuleService.removeFlowRulesById(event.subject().id());
                // }
            } else if (event.type() == ApplicationEvent.Type.APP_UNINSTALLED) {
                log.info("App {} unistalled", event.subject().id().name());

                application = event.subject();

                for (FlowRule f : flowRuleService.getFlowRulesById(application.id())) {
                    if (f.isPermanent()) {
                        uninstallRules.add(f);
                    }
                }

                if (!uninstallRules.isEmpty()) {
                    log.info("Application {} left these permanent rules: {}", application.id().name(), uninstallRules);
                    log.info("Wait 5 sec. before deleteting all its rules..");
                    startGCTimer();
                }
            } else if (event.type() == ApplicationEvent.Type.APP_INSTALLED) {
                // log.info("App Version Installed: {}",
                // event.subject().version());
                if (application != null) {
                    if (application.id() == event.subject().id()) {
                        if (!application.version().equals(event.subject().version())) {
                            log.info("App {} update installed: {}", event.subject().id().name(), event.subject()
                                    .version());

                        }
                        stopGCTimer();

                    }
                }

            } else if (event.type() == ApplicationEvent.Type.APP_ACTIVATED) {
                // FIXME: Can we trust the developer?? He/She may cannot change the versions number..
                if (application != null) {
                    if (application.id() == event.subject().id()) {
                        log.info("App {} activated, wait 5 sec. before starting garbage collection...", event.subject()
                                .id().name());
                        startGCTimer();
                    }
                }
            }
        }
    }

    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN || type == Ethernet.TYPE_ARP;
    }

    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            // Stop processing if the packet has been handled, since we
            // can't do any more to it.

            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            if (isControlPacket(ethPkt)) {
                return;
            }

            // Create Flow Rule from PacketContext
            /*FlowRule ruleToCheck = generateFlowRule(context);

            boolean isInDB = checkFlowMod(ruleToCheck);

            if (!isInDB) {
                log.debug("Rule not inside the DB");
            }*/
            // Continue with the packet
            return;

        }

    }

    private boolean checkFlowMod(FlowRule flowToCheck) {

        boolean ruleInDb = false;

        /*
         * if(db.containsKey(flowToCheck.selector())){ log.info("The rule is inside the DB"); ruleInDb = true; }
         */
        //TODO: the function should return a MMSStoreEntry list
        if (mmsDB.containsKey(flowToCheck.deviceId())) {
            Set<MMSStoreEntry> flowMMS = mmsDB.get(flowToCheck.deviceId());
            if (flowMMS != null) {
                ruleInDb = flowMMS.stream().anyMatch(rule -> rule.checkFlowMatch(flowToCheck));
            }
        }
        return ruleInDb;

    }

    private FlowRule generateFlowRule(PacketContext context) {

        Ethernet inPkt = context.inPacket().parsed();

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        selector.matchEthDst(inPkt.getDestinationMAC())
                .matchEthSrc(inPkt.getSourceMAC())
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(context.inPacket().receivedFrom().port());

        if (inPkt.getEtherType() == Ethernet.TYPE_IPV4) {
            IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
            byte ipv4Protocol = ipv4Packet.getProtocol();

            Ip4Prefix matchIp4SrcPrefix = Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(), Ip4Prefix.MAX_MASK_LENGTH);
            Ip4Prefix matchIp4DstPrefix = Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                                                            Ip4Prefix.MAX_MASK_LENGTH);
            selector.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPSrc(matchIp4SrcPrefix)
                    .matchIPDst(matchIp4DstPrefix);

            if (ipv4Protocol == IPv4.PROTOCOL_TCP) {
                TCP tcpPacket = (TCP) ipv4Packet.getPayload();
                selector.matchIPProtocol(ipv4Protocol)
                        .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                        .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
            }
            if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
                UDP udpPacket = (UDP) ipv4Packet.getPayload();
                selector.matchIPProtocol(ipv4Protocol)
                        .matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                        .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
            }
            if (ipv4Protocol == IPv4.PROTOCOL_ICMP) {
                ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();
                selector.matchIPProtocol(ipv4Protocol)
                        .matchIcmpType(icmpPacket.getIcmpType())
                        .matchIcmpCode(icmpPacket.getIcmpCode());
            }
        }

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        FlowRule returnRule = DefaultFlowRule.builder().withSelector(selector.build()).withTreatment(treatment.build())
                .fromApp(appId).makePermanent().withPriority(10)
                .forDevice(context.inPacket().receivedFrom().deviceId()).build();

        return returnRule;
    }


    @Override
    public Integer getdbSizeFromCLI(DeviceId id) {

        if (mmsDB.containsKey(id)) {
            return mmsDB.get(id).size();
        } else {
            return 0;
        }
    }

}
