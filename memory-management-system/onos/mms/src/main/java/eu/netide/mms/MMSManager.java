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
 * Antonio Marsico (amarsico@fbk.eu)
 */
package eu.netide.mms;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.netide.mms.store.DefaultMMSEntry;
import eu.netide.mms.store.MMSStoreEntry;
import org.apache.commons.collections.list.SynchronizedList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
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
import org.onlab.util.Tools;
import org.onosproject.app.ApplicationEvent;
import org.onosproject.app.ApplicationListener;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
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
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.DistributedSetBuilder;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFFlowModFailedCode;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.errormsg.OFFlowModFailedErrorMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentContext;

import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * MMS for ONOS
 */
@Service
@Component(immediate = true)
public class MMSManager implements MMSServices {

    private static final int MMS_DEFAULT_TIMEOUT = 5;
    private static final double FLOW_DELETION_THRESHOLD = 0.2;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService appAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "flowDeletionThreshold", doubleValue = FLOW_DELETION_THRESHOLD,
            label = "Configure Flow Deletion Threshold for MMS; " +
                    "default is 20 percent")
    private double flowDeletionThreshold = FLOW_DELETION_THRESHOLD;

    @Property(name = "deallocationTimeout", intValue = MMS_DEFAULT_TIMEOUT,
            label = "Configure timeout for MMS deallocation; " +
                    "default is 5 sec.")
    private int timeout = MMS_DEFAULT_TIMEOUT;

    private final InternalAppListener myAppListener = new InternalAppListener();
    private final FlowRuleListener flowListener = new InternalFlowListener();
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private final InternalOpenFlowListener oflistener = new InternalOpenFlowListener();
    private final InternalDeviceListener deviceListener = new InternalDeviceListener();

    private EventuallyConsistentMap<DeviceId, Set<MMSStoreEntry>> mmsDBSwapped = null;
    private EventuallyConsistentMap<DeviceId, List<MMSStoreEntry>> mmsDBFlowStatistics = null;
    private AsyncDistributedSet<ApplicationId> appsForMMS = null;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Application application = null;
    private Set<FlowRule> uninstallRules = Sets.newHashSet();
    private Set<FlowRule> newRules = Sets.newHashSet();

    private ScheduledFuture<?> deleteRulesScheduler = null;
    private ScheduledFuture<?> sortInternalStatisticsDB = null;

    private ScheduledExecutorService mmsScheduledTaskExecutor = Executors.newScheduledThreadPool(2);
    private ExecutorService mmsTaskExecutor = Executors.newFixedThreadPool(2);

    private AtomicBoolean flowDeletionFinished = new AtomicBoolean(true);
    private FlowId ruleToWait = null;

    private ApplicationId appId;

    @Activate
    protected void activate(ComponentContext context) {

        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("eu.netide.mms");
        readComponentConfiguration(context);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MMSStoreEntry.class)
                .register(SynchronizedList.class);

        EventuallyConsistentMapBuilder<DeviceId, Set<MMSStoreEntry>> mmsDBBuilder = storageService
                .eventuallyConsistentMapBuilder();

        EventuallyConsistentMapBuilder<DeviceId, List<MMSStoreEntry>> mmsDBStatisticsBuilder = storageService
                .eventuallyConsistentMapBuilder();

        DistributedSetBuilder<ApplicationId> appDBBuilder = storageService.setBuilder();

        mmsDBSwapped = mmsDBBuilder.withName("mms-store-swapped")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        mmsDBFlowStatistics = mmsDBStatisticsBuilder.withName("mms-store-statistics")
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        appsForMMS = appDBBuilder
                .withName("mms-app-set")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();

        for (Device device : deviceService.getAvailableDevices()) {
            List<MMSStoreEntry> flowStatistics = Collections.synchronizedList(Lists.newArrayList());
            for (FlowEntry entry : flowRuleService.getFlowEntries(device.id())) {
                if (!isONOSDefaultTreatment(entry)) {
                    flowStatistics.add(new DefaultMMSEntry(entry));
                }
            }
            mmsDBFlowStatistics.put(device.id(), flowStatistics);
        }

        packetService.addProcessor(processor, PacketProcessor.director(1));
        requestPackets();
        appAdminService.addListener(myAppListener);
        flowRuleService.addListener(flowListener);
        controller.addEventListener(oflistener);
        deviceService.addListener(deviceListener);

        log.debug("MMS store size: " + mmsDBSwapped.size());


        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    protected void deactivate() {
        stopStatisticsTimer();
        cfgService.unregisterProperties(getClass(), false);
        appAdminService.removeListener(myAppListener);
        flowRuleService.removeListener(flowListener);
        controller.removeEventListener(oflistener);
        packetService.removeProcessor(processor);
        deviceService.removeListener(deviceListener);
        mmsDBSwapped.destroy();
        mmsScheduledTaskExecutor.shutdown();
        mmsTaskExecutor.shutdown();
        processor = null;
        log.info("Stopped");
    }

    private boolean isONOSDefaultTreatment(FlowRule flowRule) {
        TrafficTreatment onosDefaultTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.CONTROLLER)
                .build();
        return flowRule.treatment().equals(onosDefaultTreatment);
    }

    /**
     * Request packet in via PacketService.
     */
    private void requestPackets() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId, Optional.empty());
    }

    private synchronized void startGCTimer() {
        stopGCTimer();
        deleteRulesScheduler = mmsScheduledTaskExecutor
                .schedule(new DeallocationRuleTask(), timeout, TimeUnit.SECONDS);
    }

    private synchronized void stopGCTimer() {
        if (deleteRulesScheduler != null) {
            deleteRulesScheduler.cancel(false);
            deleteRulesScheduler = null;
        }

    }
    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Double flowPriorityConfigured =
                getDoubleProperty(properties, "flowDeletionThreshold");
        if (flowPriorityConfigured == null) {
            flowDeletionThreshold = FLOW_DELETION_THRESHOLD;
            log.info("Flow deletion threshold is not configured, default value is {}",
                     flowDeletionThreshold);
        } else {
            flowDeletionThreshold = flowPriorityConfigured;
            log.info("Configured. Flow deletion threshold is configured to {}",
                     flowDeletionThreshold);
        }

        Integer deallocationTimeoutConfigured =
                getIntegerProperty(properties, "deallocationTimeout");
        if (deallocationTimeoutConfigured == null) {
            timeout = MMS_DEFAULT_TIMEOUT;
            log.info("Deallocation timeout is not configured, default value is {}",
                     timeout);
        } else {
            timeout = deallocationTimeoutConfigured;
            log.info("Configured. Deallocation timeout is configured to {}",
                     timeout);
        }

    }

    /**
     * Get Double property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    private static Double getDoubleProperty(Dictionary<?, ?> properties,
                                            String propertyName) {
        Double value = null;
        try {
            String s = Tools.get(properties, propertyName);
            value = isNullOrEmpty(s) ? value : Double.parseDouble(s);
        } catch (NumberFormatException | ClassCastException e) {
            value = null;
        }
        return value;
    }

    /**
     * Get Integer property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    private static Integer getIntegerProperty(Dictionary<?, ?> properties,
                                            String propertyName) {
        Integer value = null;
        try {
            String s = Tools.get(properties, propertyName);
            value = isNullOrEmpty(s) ? value : Integer.parseInt(s);
        } catch (NumberFormatException | ClassCastException e) {
            value = null;
        }
        return value;
    }

    private synchronized void startStatisticsTimer() {
        stopStatisticsTimer();
        sortInternalStatisticsDB = mmsScheduledTaskExecutor
                .scheduleAtFixedRate(new OrderStatisticsDB(), timeout, timeout, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopStatisticsTimer() {
        if (sortInternalStatisticsDB != null) {
            sortInternalStatisticsDB.cancel(false);
            sortInternalStatisticsDB = null;
        }

    }

    private class InternalOpenFlowListener
            implements OpenFlowEventListener {

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {

            switch (msg.getType()) {
                case ERROR:
                    OFErrorMsg error = (OFErrorMsg) msg;
                    if (error.getErrType() == OFErrorType.FLOW_MOD_FAILED) {
                        OFFlowModFailedErrorMsg fmFailed = (OFFlowModFailedErrorMsg) error;
                        OFFlowModFailedCode code = fmFailed.getCode();

                        log.debug("FlowMod error: {}", code);
                        try {
                            if (flowDeletionFinished.get()) {
                                //every new error must wait for the rule deletion
                                flowDeletionFinished.set(false);
                                mmsTaskExecutor.submit(new SwapRules(DeviceId.deviceId(Dpid.uri(dpid))));
                                //mmsScheduledTaskExecutor.schedule(new UnsetBoolTask(), timeout, TimeUnit.SECONDS);
                            }
                        } catch (Exception e) {
                            log.warn("Exception in SwapRule task");
                        }
                    }
                    break;
                default:
                    break;
            }

        }
    }

    // Task in order to avoid the Swap out blocking, if the switch does not send back the last FlowRule deleted
    private class UnsetBoolTask implements Runnable {

        @Override
        public void run() {
            if (!flowDeletionFinished.get()) {
                flowDeletionFinished.set(true);
                ruleToWait = null;
                log.info("Unset Swap out block");
            }
        }
    }

    private class DeallocationRuleTask implements Runnable {

        @Override
        public void run() {
            try {
                for (FlowRule f : newRules) {
                    if (uninstallRules.contains(f)) {
                        uninstallRules.remove(f);
                    }
                }

                if (!uninstallRules.isEmpty()) {

                    log.debug("FlowRules that should be deleted: {}", uninstallRules);
                    flowRuleService.removeFlowRules(Iterables.toArray(uninstallRules, FlowRule.class));

                } else {
                    log.info("There are no FlowRules to delete!");
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

    private class OrderStatisticsDB implements Runnable {

        @Override
        public void run() {
            for (DeviceId deviceToCheck : mmsDBFlowStatistics.keySet()) {
                List<MMSStoreEntry> mmsStoreEntries = mmsDBFlowStatistics.get(deviceToCheck);
                synchronized (mmsStoreEntries) {
                    Collections.sort(mmsStoreEntries);
                }
            }
        }
    }

    //Impementation of Breadth-First Search (BFS)
    //https://www.thepolyglotdeveloper.com/2015/04/various-graph-search-algorithms-using-java/
    //We need it in order to delete all the dependent rules
    private List<MMSStoreEntry> searchRuleGraph(MMSStoreEntry entry) {

        Queue<MMSStoreEntry> queue = new LinkedList<MMSStoreEntry>();
        List<MMSStoreEntry> listToDelete = Lists.newArrayList();
        MMSStoreEntry copyEntry = new DefaultMMSEntry(entry);

        copyEntry.setVisited(true);
        queue.add(copyEntry);

        while(!queue.isEmpty()) {
            MMSStoreEntry v = queue.poll();
            for(MMSStoreEntry w : v.getRuleParents()) {
                MMSStoreEntry copyInside = new DefaultMMSEntry(w);
                if(!copyInside.getVisited()) {
                    copyInside.setVisited(true);
                    queue.add(copyInside);
                    listToDelete.add(w);
                }
            }
        }

        return listToDelete;
    }

    private List<MMSStoreEntry> potentialParents(MMSStoreEntry rule, List<MMSStoreEntry> deviceFlowTable) {

        List<MMSStoreEntry> parentRules = deviceFlowTable.stream()
                .filter(entry -> entry.priority() < rule.priority()).collect(Collectors.toList());

        return parentRules;
    }

    private List<MMSStoreEntry> addParents(MMSStoreEntry rule, List<MMSStoreEntry> potentialParentRules) {

        List<MMSStoreEntry> parents = Lists.newArrayList();
        MMSStoreEntry ruleToCheck = rule;

        for (MMSStoreEntry potentialParent : potentialParentRules) {
            if(ruleToCheck.intersect(potentialParent) != DefaultTrafficSelector.emptySelector()) {
                parents.add(potentialParent);
                TrafficSelector diffSelector = ruleToCheck.diff(potentialParent);

                if(diffSelector == DefaultTrafficSelector.emptySelector()) {
                    //We can exit if there is an empty selector
                    break;
                } else {
                    FlowRule tempRule = DefaultFlowRule.builder()
                            .forDevice(rule.deviceId())
                            .fromApp(appId)
                            .withPriority(rule.priority())
                            .makePermanent()
                            .withSelector(diffSelector)
                            .withTreatment(rule.treatment())
                            .build();
                    ruleToCheck = new DefaultMMSEntry(tempRule);
                }
            }
        }
        return parents;
    }

    private class DeviceDependencyCalculator implements Runnable {

        private DeviceId deviceToCheck;

        public DeviceDependencyCalculator(DeviceId deviceId) {
            this.deviceToCheck = deviceId;
        }

        @Override
        public void run() {

            List<MMSStoreEntry> deviceFlowTable = mmsDBFlowStatistics.get(deviceToCheck);

            synchronized (deviceFlowTable) {
                for (MMSStoreEntry entryToCheck : deviceFlowTable) {

                    List<MMSStoreEntry> potentialParentList = potentialParents(entryToCheck, deviceFlowTable);

                    if (potentialParentList.size() > 0) {
                        Collections.sort(potentialParentList, new FlowRuleComparator());
                        entryToCheck.setRuleParents(addParents(entryToCheck, potentialParentList));
                    }
                }
            }
        }

    }

    private class FlowRuleComparator implements Comparator<FlowRule> {
       @Override
        public int compare(FlowRule rule1, FlowRule rule2) {
           Integer priority1 = rule1.priority();
           Integer priority2 = rule2.priority();
           return priority1.compareTo(priority2);
       }
    }

    private class SwapRules implements Runnable {

        private DeviceId deviceId;
        
        public SwapRules(DeviceId deviceId) {
            this.deviceId = deviceId;
        }
        @Override
        public void run() {

            //Future<?> dependecyCalculatorStatus = mmsTaskExecutor.submit(new DeviceDependencyCalculator(deviceId));

            List<MMSStoreEntry> listToCheck = mmsDBFlowStatistics.get(deviceId);

            List<FlowRule> rulesToDelete = Lists.newArrayList();

            int threshold = (int) (flowDeletionThreshold * listToCheck.size());

            Set<MMSStoreEntry> swappedSet;

            int lastRule = 0;

            List<MMSStoreEntry> entriesWithDependencies = Lists.newArrayList();
            try {
                //The dependency task is finished
                //if (dependecyCalculatorStatus.get() == null) {
                    synchronized (listToCheck) {
                        Collections.sort(listToCheck);
                        for (int i = 0; i < threshold; i++) {

                            MMSStoreEntry entry = listToCheck.get(i);
                            entriesWithDependencies.add(entry);
                            List<MMSStoreEntry> parents = entry.getRuleParents();

                            for (MMSStoreEntry parentEntry : parents) {
                                if (!entriesWithDependencies.contains(parentEntry)) {
                                    entriesWithDependencies.add(parentEntry);
                                    i++;
                                    List<MMSStoreEntry> graphSearchResults = searchRuleGraph(parentEntry);
                                    for (MMSStoreEntry graphElement : graphSearchResults) {
                                        if (!entriesWithDependencies.contains(graphElement)) {
                                            entriesWithDependencies.add(graphElement);
                                            i++;
                                        }
                                    }
                                }
                            }

                        }
                    }

                    for (int i = 0; i < entriesWithDependencies.size(); i++) {
                        MMSStoreEntry entry = entriesWithDependencies.get(i);
                        //Swap only if permanent, otherwise delete only
                        if (entry.timeout() == 0) {
                            if (mmsDBSwapped.containsKey(deviceId)) {
                                swappedSet = mmsDBSwapped.get(deviceId);
                                entry.resetLastPacket();
                                swappedSet.add(entry);
                                lastRule = i;
                            } else {
                                swappedSet = Sets.newConcurrentHashSet();
                                swappedSet.add(entry);
                                mmsDBSwapped.put(deviceId, swappedSet);
                            }
                        } else {
                            //Entries that are reactive
                            lastRule = i;
                        }
                        rulesToDelete.add(new DefaultFlowRule(entry));
                    }

                    ruleToWait = rulesToDelete.get(lastRule).id();

                    flowRuleService.removeFlowRules(Iterables.toArray(rulesToDelete, FlowRule.class));

                    log.info("There were {} less used rules!", rulesToDelete.size());
                //}
            } catch (Exception e) {
                log.warn("Exception in calculate dependencies task {}", e);
            }
        }
    }

    private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {

            if (!isONOSDefaultTreatment(event.subject())) {

                if (event.type() == FlowRuleEvent.Type.RULE_UPDATED) {

                    FlowEntry f = (FlowEntry) event.subject();

                    if (mmsDBFlowStatistics.containsKey(f.deviceId())) {

                        List<MMSStoreEntry> flowStatistics = mmsDBFlowStatistics.get(f.deviceId());

                        synchronized (flowStatistics) {
                            //int index = 0;
                            boolean flowRuleIsPresent = false;
                            for (MMSStoreEntry entryToUpdate : flowStatistics) {
                                if (entryToUpdate.exactMatch(f)) {
                                    entryToUpdate.addPackets(f.packets());
                                    entryToUpdate.calculateExponentialWeightedAverage();
                                    flowRuleIsPresent = true;
                                    break;
                                }
                            }
                            // In case we miss sth from the device
                            if (!flowRuleIsPresent) {
                                MMSStoreEntry flowEntry = new DefaultMMSEntry(f);
                                flowStatistics.add(flowEntry);
                            }
                        }
                    }

                }

                if (event.type() == FlowRuleEvent.Type.RULE_ADDED) {

                    FlowEntry f = (FlowEntry) event.subject();

                    MMSStoreEntry flowEntry = new DefaultMMSEntry(f);

                    if (mmsDBFlowStatistics.containsKey(f.deviceId())) {

                        if (!checkFlowInDB(flowEntry)) {
                            List<MMSStoreEntry> flowStatistics = mmsDBFlowStatistics.get(f.deviceId());
                            synchronized (flowStatistics) {
                                flowStatistics.add(flowEntry);
                            }
                        }

                    }

                }

                if (event.type() == FlowRuleEvent.Type.RULE_REMOVED) {

                    FlowRule f = event.subject();

                    if (mmsDBFlowStatistics.containsKey(f.deviceId())) {

                        List<MMSStoreEntry> flowStatistics = mmsDBFlowStatistics.get(f.deviceId());
                        synchronized (flowStatistics) {
                            for (int i = 0; i < flowStatistics.size(); i++) {
                                MMSStoreEntry entryToUpdate = flowStatistics.get(i);
                                if (entryToUpdate.id().equals(f.id())) {
                                    if (ruleToWait != null) {
                                        if (ruleToWait.equals(f.id()) && !flowDeletionFinished.get()) {
                                            log.info("Last rule deleted");
                                            flowDeletionFinished.set(true);
                                            ruleToWait = null;
                                        }
                                    }
                                    flowStatistics.remove(i);
                                }
                            }
                        }
                    }

                    /*if (flowDeletionFinished.get() && mmsDBSwapped.containsKey(f.deviceId())) {

                        Set<MMSStoreEntry> entries = mmsDBSwapped.get(f.deviceId());

                        for (MMSStoreEntry entryToUpdate : entries) {

                            if (entryToUpdate.exactMatch(f)) {
                                entries.remove(entryToUpdate);
                                break;
                            }
                        }

                    }*/
                }
            }

            if (application != null) {
                FlowRule f = event.subject();
                if ((event.type() == FlowRuleEvent.Type.RULE_ADD_REQUESTED) && (application.id().id() == f.appId())) {
                    if (f.isPermanent()) {
                        log.debug("Saving new flow mods for app {}", application.id().name());
                        newRules.add(f);
                    }

                }
            }

        }
    }

    private boolean checkFlowInDB(FlowRule flowRule) {

        List<MMSStoreEntry> flowStatistics = mmsDBFlowStatistics.get(flowRule.deviceId());
        synchronized (flowStatistics) {
            return flowStatistics.stream().anyMatch(f -> flowRule.id().equals(f.id()));
        }
    }

    private class InternalAppListener implements ApplicationListener {

        @Override
        public void event(ApplicationEvent event) {
            // We care only for the app that the administrator requires us to check
            try {
                if (!appsForMMS.contains(event.subject().id()).get())
                    return;
            } catch (Exception e) {
                log.warn("Exception getting MMS apps database...");
            }

            if (event.type() == ApplicationEvent.Type.APP_DEACTIVATED) {
                log.info("App {} deactived, Garbage Collector is starting...", event.subject().id().name());
                flowRuleService.removeFlowRulesById(event.subject().id());
                removeSwappedFlowRulesById(event.subject().id());

            } else if (event.type() == ApplicationEvent.Type.APP_UNINSTALLED) {
                log.info("App {} unistalled", event.subject().id().name());

                application = event.subject();

                removeSwappedFlowRulesById(application.id());

                for (FlowRule f : flowRuleService.getFlowRulesById(application.id())) {
                    uninstallRules.add(f);

                }

                if (!uninstallRules.isEmpty()) {
                    log.debug("Application {} left these permanent rules: {}", application.id().name(), uninstallRules);
                    log.info("Wait 5 sec. before deleting all its rules..");
                    startGCTimer();
                }
            } else if (event.type() == ApplicationEvent.Type.APP_INSTALLED) {

                log.info("App Version Installed: {}", event.subject().version());

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
                // FIXME: Can we trust the developer?? He/She could not change the version number!
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

    public void removeSwappedFlowRulesById(ApplicationId id) {

        Set<MMSStoreEntry> flowEntries = Sets.newHashSet();
        for (DeviceId d : mmsDBSwapped.keySet()) {
            for (MMSStoreEntry mmsSwappedEntry : mmsDBSwapped.get(d)) {
                if (mmsSwappedEntry.appId() == id.id()) {
                    flowEntries.add(mmsSwappedEntry);
                }
            }
            mmsDBSwapped.get(d).removeAll(flowEntries);
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

            FlowRule ruleToCheck = generateFlowRule(context);

            Set<MMSStoreEntry> rulesSwapped = findSwapped(ruleToCheck);
            try {
                if(rulesSwapped != null) {
                    //let's install the swapped rules...

                    for (MMSStoreEntry entry : rulesSwapped) {
                        FlowRule ruleToApply = new DefaultFlowRule(entry);
                        flowRuleService.applyFlowRules(ruleToApply);
                    }

                    //Block the packet context, so other apps cannot
                    //could generate new unwanted rules.
                    context.block();
                }
            } catch (Exception e) {
                log.warn("Error handling CheckSwappedRules method");
            }
        }

    }

    private Set<MMSStoreEntry> findSwapped(FlowRule flowToCheck) {
        if (mmsDBSwapped.containsKey(flowToCheck.deviceId())) {

            Set<MMSStoreEntry> ruleSwappedMatch = mmsDBSwapped.get(flowToCheck.deviceId())
                    .stream().filter(rule -> rule.checkFlowMatch(flowToCheck)).collect(Collectors.toSet());

            if (ruleSwappedMatch.size() > 0) {
                mmsDBSwapped.get(flowToCheck.deviceId()).removeAll(ruleSwappedMatch);
                List<MMSStoreEntry> listStats = mmsDBFlowStatistics.get(flowToCheck.deviceId());
                synchronized (listStats) {
                    listStats.addAll(ruleSwappedMatch);
                }
                return ruleSwappedMatch;
            }

        }
        return null;
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

        FlowRule returnRule = DefaultFlowRule.builder()
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .fromApp(appId)
                .makePermanent()
                .withPriority(10)
                .forDevice(context.inPacket().receivedFrom().deviceId())
                .build();

        return returnRule;
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {

            switch (event.type()) {
                case DEVICE_ADDED:
                    List<MMSStoreEntry> flowStatistics = Collections.synchronizedList(Lists.newArrayList());
                    mmsDBFlowStatistics.put(event.subject().id(), flowStatistics);
                    break;
                case DEVICE_REMOVED:
                    mmsDBFlowStatistics.remove(event.subject().id());
                    mmsDBSwapped.remove(event.subject().id());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public Integer getdbSizeFromCLI(DeviceId id) {

        if (mmsDBSwapped.containsKey(id)) {
            return mmsDBSwapped.get(id).size();
        } else {
            return 0;
        }

    }

    @Override
    public void addAppToMMS(ApplicationId app) {

        try {
            if (!appsForMMS.contains(app).get()) {
                appsForMMS.add(app);
            }
        } catch (Exception e) {

        }

    }

    @Override
    public void deleteAppFromMMS(ApplicationId app) {

        try {
            if (appsForMMS.contains(app).get()) {
                appsForMMS.remove(app);
            }
        } catch (Exception e) {

        }

    }

    @Override
    public Set<ApplicationId> getApplicationsMMS() {
        try {

            Set<ApplicationId> returnSet = Sets.newHashSet(appsForMMS.getAsImmutableSet().get());
            return returnSet;

        } catch (Exception e) {
            return null;
        }
    }
}
