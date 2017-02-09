/*
 * Copyright (c) 2016, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 * Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 * Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.statefullfirewall;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onlab.packet.TpPort;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_PRIORITY = 10;

    @Property(name = "flowTimeout", intValue = DEFAULT_TIMEOUT,
            label = "Configure Flow Timeout for installed flow rules; " +
                "default is 10 sec")
    private int flowTimeout = DEFAULT_TIMEOUT;

    @Property(name = "flowPriority", intValue = DEFAULT_PRIORITY,
            label = "Configure Flow Priority for installed flow rules; " +
                "default is 10")
    private int flowPriority = DEFAULT_PRIORITY;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private ApplicationId appId;

	//Network Variables
	private final DeviceId edgeRouter = DeviceId.deviceId("of:0000000000000001");

	private final PortNumber exitPortWorld = PortNumber.portNumber(1);
	private final PortNumber exitPortNet = PortNumber.portNumber(2);
	private final Ip4Prefix netAddress = Ip4Prefix.valueOf("192.168.3.0/24");

	// private final PortNumber exitPortSmtp = PortNumber.portNumber(3);
	private final Ip4Prefix smtpAddress = Ip4Prefix.valueOf("192.168.2.2/32");

	//PortNumber exitPortwww = PortNumber.portNumber(2);
	private final Ip4Prefix wwwAddress = Ip4Prefix.valueOf("192.168.2.1/32");

	private final Ip4Prefix server = Ip4Prefix.valueOf("192.168.1.1/32");

    @Activate
    protected void activate() {
    	cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("eu.netide.statefullfirewall");
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 1);
        //installFirewallRules();
        requestPackests();
    	log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    protected void deactivate() {
    	cfgService.unregisterProperties(getClass(), false);
    	packetService.removeProcessor(processor);
    	processor = null;
        log.info("Stopped");
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestPackests() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE,
                                     appId);
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            // Stop processing if the packet has been handled, since we
            // can't do any more to it.

            if (context.isHandled()) {
                return;
            }

            //Don't process packets coming from other devices

            //DeviceId edgeRouter = DeviceId.deviceId("of:0000000000000001");
            //DeviceId

            if (!edgeRouter.equals(context.inPacket().receivedFrom().deviceId())) {
            	//log.info("No edge {}", context.inPacket().receivedFrom().deviceId());
            	return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
            	IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
                byte ipv4Protocol = ipv4Packet.getProtocol();

                Ip4Prefix matchIp4SrcPrefix =
                        Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                                          24);


                if (matchIp4SrcPrefix.equals(netAddress) && ipv4Protocol == IPv4.PROTOCOL_TCP) {

                	log.info("Match ip src");
                	TCP tcpPacket = (TCP) ipv4Packet.getPayload();
                	int tcpPortWWW = 80;
                	int tcpPortSSL = 443;

                	if((tcpPacket.getDestinationPort() != tcpPortWWW) && (tcpPacket.getDestinationPort() != tcpPortSSL)) {
                		log.debug("TCP Blocked");
                		context.block();
                		return;
                	}
            	}
                else
                {
                	//Block all the remaining traffic
                	context.block();
                	return;
                }
            }

            // Bail if this is deemed to be a control packet.
            if (isControlPacket(ethPkt)) {
            	//log.info("Control Packet");
                return;
            }

            HostId id = HostId.hostId(ethPkt.getDestinationMAC());

            // Do not process link-local addresses in any way.
            if (id.mac().isLinkLocal()) {
                return;
            }

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(id);
            if (dst == null) {
                //flood(context);
                return;
            }

            // Are we on an edge switch that our destination is on? If so,
            // simply forward out to the destination and bail.
            if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                if (!context.inPacket().receivedFrom().port().equals(dst.location().port())) {
                    installBidirectionalFirewallRule(context, dst.location().port());
                }
                return;
            }

            // Otherwise, get a set of paths that lead from here to the
            // destination edge switch.

            Set<Path> paths =
                topologyService.getPaths(topologyService.currentTopology(),
                                         pkt.receivedFrom().deviceId(),
                                         dst.location().deviceId());
            log.info("Current topology: {}", dst.location().deviceId());
            if (paths.isEmpty()) {
                // If there are no paths, flood and bail.
                //flood(context);
                return;
            }

            // Otherwise, pick a path that does not lead back to where we
            // came from; if no such path, flood and bail.
            Path path = pickForwardPath(paths, pkt.receivedFrom().port());
            if (path == null) {
                log.warn("Doh... don't know where to go... {} -> {} received on {}",
                         ethPkt.getSourceMAC(), ethPkt.getDestinationMAC(),
                         pkt.receivedFrom());
                //flood(context);
                return;
            }

            // Otherwise forward and be done with it.
            installBidirectionalFirewallRule(context, path.src().port());

        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    // Selects a path from the given set that does not lead back to the
    // specified port.
    private Path pickForwardPath(Set<Path> paths, PortNumber notToPort) {
        for (Path path : paths) {
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return null;
    }

    private void installBidirectionalFirewallRule (PacketContext context, PortNumber portNumber){

    	Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilderOut = DefaultTrafficSelector.builder();

        TrafficSelector.Builder selectorBuilderIn = DefaultTrafficSelector.builder();

        //Create the bidirectional rule

        if(inPkt.getEtherType() == Ethernet.TYPE_IPV4) {
            IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
            byte ipv4Protocol = ipv4Packet.getProtocol();
            Ip4Prefix matchIp4SrcPrefix =
                Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                                  Ip4Prefix.MAX_MASK_LENGTH);
            Ip4Prefix matchIp4DstPrefix =
                Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                                  Ip4Prefix.MAX_MASK_LENGTH);
            selectorBuilderOut.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPSrc(matchIp4SrcPrefix)
                    .matchIPDst(matchIp4DstPrefix);

            selectorBuilderIn.matchEthType(Ethernet.TYPE_IPV4)
            		.matchIPSrc(matchIp4DstPrefix)
            		.matchIPDst(matchIp4SrcPrefix);

            if (ipv4Protocol == IPv4.PROTOCOL_TCP) {
                TCP tcpPacket = (TCP) ipv4Packet.getPayload();
                selectorBuilderOut.matchIPProtocol(ipv4Protocol)
                        .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                        .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));

                selectorBuilderIn.matchIPProtocol(ipv4Protocol)
                		.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
                		.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
            }
        }

    	TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
                .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilderOut.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                                     forwardingObjective);

        //Create the rule from outbound to inbound

        TrafficTreatment treatmentIn = DefaultTrafficTreatment.builder()
                .setOutput(context.inPacket().receivedFrom().port())
                .build();

        ForwardingObjective forwardingObjectiveIn = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilderIn.build())
                .withTreatment(treatmentIn)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                                     forwardingObjectiveIn);


    	packetOut(context, portNumber);
    }

    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN || type == Ethernet.TYPE_ARP;
    }

    private void installFirewallRules() {

    	//World traffic to network
    	TrafficSelector.Builder selectorBuilderGeneral = DefaultTrafficSelector.builder();
    	TrafficTreatment.Builder treatmentGeneral = DefaultTrafficTreatment.builder();

        //World traffic to WWW 80

    	selectorBuilderGeneral = DefaultTrafficSelector.builder()
		    	.matchEthType(Ethernet.TYPE_IPV4)
				.matchIPDst(wwwAddress)
				.matchIPProtocol(IPv4.PROTOCOL_TCP)
				.matchTcpDst(TpPort.tpPort(80));

    	treatmentGeneral = DefaultTrafficTreatment.builder()
    			.setOutput(exitPortNet);

    	ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
		        .withSelector(selectorBuilderGeneral.build())
		        .withTreatment(treatmentGeneral.build())
		        .withPriority(flowPriority)
		        .withFlag(ForwardingObjective.Flag.VERSATILE)
		        .fromApp(appId)
		        .add();
		flowObjectiveService.forward(edgeRouter,
		        forwardingObjective);

        //World traffic to WWW 443

    	selectorBuilderGeneral = DefaultTrafficSelector.builder()
		    	.matchEthType(Ethernet.TYPE_IPV4)
				.matchIPDst(wwwAddress)
				.matchIPProtocol(IPv4.PROTOCOL_TCP)
				.matchTcpDst(TpPort.tpPort(443));

    	treatmentGeneral = DefaultTrafficTreatment.builder()
    			.setOutput(exitPortNet);

    	forwardingObjective = DefaultForwardingObjective.builder()
		        .withSelector(selectorBuilderGeneral.build())
		        .withTreatment(treatmentGeneral.build())
		        .withPriority(flowPriority)
		        .withFlag(ForwardingObjective.Flag.VERSATILE)
		        .fromApp(appId)
		        .add();
		flowObjectiveService.forward(edgeRouter,
		        forwardingObjective);

		//World traffic to SMTP

    	selectorBuilderGeneral = DefaultTrafficSelector.builder()
		    	.matchEthType(Ethernet.TYPE_IPV4)
				.matchIPDst(smtpAddress)
				.matchIPProtocol(IPv4.PROTOCOL_TCP)
				.matchTcpDst(TpPort.tpPort(25));

    	treatmentGeneral = DefaultTrafficTreatment.builder()
    			.setOutput(exitPortNet);

    	forwardingObjective = DefaultForwardingObjective.builder()
		        .withSelector(selectorBuilderGeneral.build())
		        .withTreatment(treatmentGeneral.build())
		        .withPriority(flowPriority)
		        .withFlag(ForwardingObjective.Flag.VERSATILE)
		        .fromApp(appId)
		        .add();
		flowObjectiveService.forward(edgeRouter,
		        forwardingObjective);



    }

}
