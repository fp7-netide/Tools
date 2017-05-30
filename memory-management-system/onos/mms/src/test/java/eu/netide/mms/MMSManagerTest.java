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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.netide.mms.store.DefaultMMSEntry;
import eu.netide.mms.store.MMSStoreEntry;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ComponentContextAdapter;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.TestApplicationId;
import org.onosproject.app.ApplicationAdminServiceAdapter;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowMessageListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.store.service.DistributedSetBuilder;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.StorageServiceAdapter;
import org.onosproject.store.service.TestDistributedSet;
import org.onosproject.store.service.TestEventuallyConsistentMap;
import org.osgi.service.component.ComponentContext;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Set of tests of the ONOS application component.
 */
public class MMSManagerTest {

    private MMSManager component;

    private PacketProcessor packetProcessor;

    private FlowRuleListener flowRuleListener;

    private static final int APPLICATION_ID = 1;

    private static final String FOO_COMPONENT = "fooComponent";

    private final Logger log = getLogger(getClass());

    private static final int SIZE_EXPECTED_CHILD = 1;

    private static final int SIZE_EXPECTED_PARENT = 0;

    private static final long PACKETS = 10;

    private static final String APP_NAME = "eu.netide.mms";

    public static final ComponentContextAdapter MMS_CONTEXT = new ComponentContextAdapter() {
        @Override
        public Dictionary getProperties() {
            Dictionary<String, String> properties = new Hashtable<>();
            properties.put("flowDeletionThreshold",
                           Double.toString(0.2));
            properties.put("deallocationTimeout",
                           Integer.toString(5));
            return properties;
        }
    };

    @Before
    public void setUp() {
        component = new MMSManager();
        component.coreService = new TestCoreService();

        ComponentConfigService mockConfigService =
                EasyMock.createMock(ComponentConfigService.class);

        component.cfgService = mockConfigService;
        component.packetService = new TestPacketService();
        component.appAdminService = new TestAppService();
        component.storageService = new TestStorageService();
        component.flowRuleService = new TestFlowService();
        component.controller = new TestOpenFlowController();

/*        ComponentContext mockContext = EasyMock.createMock(ComponentContext.class);
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("flowDeletionThreshold",
                       Double.toString(0.2));
        properties.put("deallocationTimeout",
                       Integer.toString(5));
        expect(mockContext.getProperties()).andReturn(properties);
        replay(mockContext);*/

        component.activate(MMS_CONTEXT);

    }

    @After
    public void tearDown() {
        component.deactivate();
    }

    @Test
    public void testMMSEntry() {

        FlowRule rule = genFlow("of:0000000000000002",1,2);

        DefaultMMSEntry entry = new DefaultMMSEntry(rule);

        entry.addPackets(PACKETS);

        assertEquals(DeviceId.deviceId("of:0000000000000002"), entry.deviceId());
        assertEquals(APPLICATION_ID, entry.appId());
        assertEquals(PACKETS, entry.packets().get(0).longValue());
    }

    @Test
    public void testRuleDependenciesTrue() {

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.0.2/32"))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry childRule = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(20))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();
        MMSStoreEntry parentRule = new DefaultMMSEntry(rule2);

        assertTrue("The child rule must be dependent on the parent", childRule.isParent(parentRule));
    }

    @Test
    public void testRuleDependenciesWithIPTrue() {

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.2/32"))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry childRule = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.0/24"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.0/24"))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(20))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();
        MMSStoreEntry parentRule = new DefaultMMSEntry(rule2);

        assertTrue("The child rule must be dependent on the parent", childRule.isParent(parentRule));

        //Check if we invert the parent and child, the rules are not dependent any more, due to priority
        assertFalse("The child rule must not be dependent on the parent", parentRule.isParent(childRule));
    }

    @Test
    public void testRuleDependenciesError() {

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.0.2/32"))
                .matchTcpDst(TpPort.tpPort(20))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry childRule = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(100))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();
        MMSStoreEntry parentRule = new DefaultMMSEntry(rule2);

        assertEquals("The child rule must not be dependent on the parent", DefaultTrafficSelector.emptySelector(),
                     childRule.intersect(parentRule));
    }

    @Test
    public void testIntersection() {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.2/32"))
                .matchTcpDst(TpPort.tpPort(20))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry childRule = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.0/24"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.0/24"))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpSrc(TpPort.tpPort(120))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();
        MMSStoreEntry parentRule = new DefaultMMSEntry(rule2);

        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.2/32"))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(20))
                .matchTcpSrc(TpPort.tpPort(120))
                .build();

        assertEquals("The selectors must be equals", expectedSelector, childRule.intersect(parentRule));
    }

    @Test
    public void testIPIntersection() {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry childRule = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPSrc(IpPrefix.valueOf("192.168.1.0/24"))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();
        MMSStoreEntry parentRule = new DefaultMMSEntry(rule2);

        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.0/24"))
                .build();

        assertEquals("The selectors must be equals", expectedSelector, childRule.intersect(parentRule));
    }

    @Test
    public void testRuleDependenciesWithIPFalse() {

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.20.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.32.2/32"))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry childRule = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.0/24"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.0/24"))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(20))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();
        MMSStoreEntry parentRule = new DefaultMMSEntry(rule2);

        assertEquals("The child rule must not be dependent on the parent", DefaultTrafficSelector.emptySelector(),
                     childRule.intersect(parentRule));
    }

    @Test
    public void testCacheFlowCoVisorRules() {

        //flowRuleListener.event(new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADDED, rule1));
        //flowRuleListener.event(new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADDED, rule2));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("2.0.0.1/32"))
                .matchIPSrc(IpPrefix.valueOf("1.0.0.0/24"))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(5)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry mmsRule1 = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("2.0.0.2/32"))
                .matchIPSrc(IpPrefix.valueOf("1.0.0.0/24"))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(4)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry mmsRule2 = new DefaultMMSEntry(rule2);

        TrafficSelector testSelector3 = DefaultTrafficSelector.builder()
                .matchIPSrc(IpPrefix.valueOf("1.0.0.0/24"))
                .build();

        FlowRule rule3 = DefaultFlowRule.builder()
                .withSelector(testSelector3)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(3)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry mmsRule3 = new DefaultMMSEntry(rule3);

        TrafficSelector testSelector4 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("2.0.0.1/32"))
                .build();

        FlowRule rule4 = DefaultFlowRule.builder()
                .withSelector(testSelector4)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(2)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry mmsRule4 = new DefaultMMSEntry(rule4);

        TrafficSelector testSelector5 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("2.0.0.2/32"))
                .build();

        FlowRule rule5 = DefaultFlowRule.builder()
                .withSelector(testSelector5)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(1)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry mmsRule5 = new DefaultMMSEntry(rule5);



        List<MMSStoreEntry> deviceFlowTable = Lists.newArrayList(mmsRule1, mmsRule2, mmsRule3, mmsRule4, mmsRule5);

        for (MMSStoreEntry entryToCheck : deviceFlowTable) {

            try {
                List<MMSStoreEntry> potentialParentList = TestUtils
                        .callMethod(component, "potentialParents", new Class<?>[]
                                {MMSStoreEntry.class, List.class}, entryToCheck, deviceFlowTable);
                //potentialParents(entryToCheck, deviceFlowTable);
                if (potentialParentList.size() > 0) {
                    List<MMSStoreEntry> rulesToAdd = TestUtils
                            .callMethod(component, "addParents", new Class<?>[]
                                    {MMSStoreEntry.class, List.class}, entryToCheck, potentialParentList);
                    entryToCheck.setRuleParents(rulesToAdd);
                    //System.out.println(entryToCheck);
                }

            } catch (Exception e) {
                System.err.println("Error getting the private method "+e);
            }

        }

        assertEquals("The R1 must have 1 parents", 1, deviceFlowTable.get(0).getRuleParents().size());
        assertEquals("The R2 must have 1 parents", 1, deviceFlowTable.get(1).getRuleParents().size());
        assertEquals("The R3 must have 2 parents", 2, deviceFlowTable.get(2).getRuleParents().size());
        assertEquals("The R4 must have 0 parents", 0, deviceFlowTable.get(3).getRuleParents().size());
        assertEquals("The R5 must have 0 parents", 0, deviceFlowTable.get(4).getRuleParents().size());

        try {
            List<MMSStoreEntry> entryToDelete = TestUtils
                    .callMethod(component, "searchRuleGraph", new Class<?>[]
                            {MMSStoreEntry.class}, deviceFlowTable.get(2));

            assertEquals("The chain for R3 is incorrect", 2, entryToDelete.size());

            entryToDelete = TestUtils
                    .callMethod(component, "searchRuleGraph", new Class<?>[]
                            {MMSStoreEntry.class}, deviceFlowTable.get(0));

            assertEquals("The chain for R3 is incorrect", 3, entryToDelete.size());

        } catch (TestUtils.TestUtilsException e) {
            System.err.println("Error getting the private method "+e);
        }
    }

    @Test
    public void testDiff() {

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1));

        TrafficSelector testSelector1 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.2/32"))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(20))
                .build();

        FlowRule rule1 = DefaultFlowRule.builder()
                .withSelector(testSelector1)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        MMSStoreEntry childRule = new DefaultMMSEntry(rule1);

        TrafficSelector testSelector2 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.0/24"))
                .matchIPSrc(IpPrefix.valueOf("192.168.1.0/24"))
                .build();

        FlowRule rule2 = DefaultFlowRule.builder()
                .withSelector(testSelector2)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();
        MMSStoreEntry parentRule = new DefaultMMSEntry(rule2);

        assertEquals("Child must contains all the parent match", DefaultTrafficSelector.emptySelector(),
                     childRule.diff(parentRule));

        TrafficSelector testSelector3 = DefaultTrafficSelector.builder()
                .matchIPDst(IpPrefix.valueOf("192.168.0.3/32"))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(20))
                .build();

        FlowRule rule3 = DefaultFlowRule.builder()
                .withSelector(testSelector3)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(10)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        childRule = new DefaultMMSEntry(rule3);

        TrafficSelector testSelector4 = DefaultTrafficSelector.builder()
                .matchTcpSrc(TpPort.tpPort(100))
                .build();

        FlowRule rule4 = DefaultFlowRule.builder()
                .withSelector(testSelector4)
                .withTreatment(treatment.build())
                .fromApp(new DefaultApplicationId(1, "eu.netide.mms"))
                .makePermanent()
                .withPriority(8)
                .forDevice(DeviceId.deviceId("of:0000000000000002"))
                .build();

        parentRule = new DefaultMMSEntry(rule4);

        TrafficSelector expectedSelector = DefaultTrafficSelector.builder()
                .matchTcpSrc(TpPort.tpPort(100))
                .build();

        assertEquals("Child must contains all the parent match", testSelector3,
                     childRule.diff(parentRule));

    }

    private FlowRule genFlow(String d, long inPort, long outPort) {
        DeviceId device = DeviceId.deviceId(d);
        TrafficSelector ts = DefaultTrafficSelector.builder().matchInPort(PortNumber.portNumber(inPort)).build();
        TrafficTreatment tt = DefaultTrafficTreatment.builder()
                .add(Instructions.createOutput(PortNumber.portNumber(outPort))).build();
        return new DefaultFlowRule(device, ts, tt, 1, new DefaultApplicationId(APPLICATION_ID, APP_NAME),
                                   50000, true, FlowRuleExtPayLoad.flowRuleExtPayLoad(new byte[5]));
    }

    /**
     * Mocks the DefaultPacket context.
     */
    private final class TestPacketContext extends DefaultPacketContext {
        private TestPacketContext(long time, InboundPacket inPkt,
                                  OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {
            // We don't send anything out.
        }
    }

    /**
     * Keeps a reference to the PacketProcessor and verifies the OutboundPackets.
     */
    private class TestPacketService extends PacketServiceAdapter {

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            packetProcessor = processor;
        }
    }

    /**
     * Mocks the CoreService.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new TestApplicationId(name);
        }

    }

    private class TestAppService extends ApplicationAdminServiceAdapter {

    }

    private class TestStorageService extends StorageServiceAdapter {

        @Override
        public <K, V> EventuallyConsistentMapBuilder<K, V> eventuallyConsistentMapBuilder() {
            return TestEventuallyConsistentMap.builder();
        }
        @Override
        public <E> DistributedSetBuilder<E> setBuilder() {
            return TestDistributedSet.builder();
        }


    }

    private class TestFlowService extends FlowRuleServiceAdapter {

        @Override
        public void addListener(FlowRuleListener listener) {
            flowRuleListener = listener;
        }

    }

    private class TestOpenFlowController implements OpenFlowController {

        @Override
        public Iterable<OpenFlowSwitch> getSwitches() {
            return null;
        }

        @Override
        public Iterable<OpenFlowSwitch> getMasterSwitches() {
            return null;
        }

        @Override
        public Iterable<OpenFlowSwitch> getEqualSwitches() {
            return null;
        }

        @Override
        public OpenFlowSwitch getSwitch(Dpid dpid) {
            return null;
        }

        @Override
        public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
            return null;
        }

        @Override
        public OpenFlowSwitch getEqualSwitch(Dpid dpid) {
            return null;
        }


        @Override
        public void addListener(OpenFlowSwitchListener listener) {

        }

        @Override
        public void removeListener(OpenFlowSwitchListener listener) {

        }

        @Override
        public void addMessageListener(OpenFlowMessageListener openFlowMessageListener) {

        }

        @Override
        public void removeMessageListener(OpenFlowMessageListener openFlowMessageListener) {

        }

        @Override
        public void addPacketListener(int priority, PacketListener listener) {

        }

        @Override
        public void removePacketListener(PacketListener listener) {

        }

        @Override
        public void addEventListener(OpenFlowEventListener listener) {

        }

        @Override
        public void removeEventListener(OpenFlowEventListener listener) {

        }

        @Override
        public void write(Dpid dpid, OFMessage msg) {

        }

        @Override
        public void processPacket(Dpid dpid, OFMessage msg) {

        }

        @Override
        public void setRole(Dpid dpid, RoleState role) {

        }
    }

    /*
 * Mockup class for the config service.
 */
    private class TestConfigService extends ComponentConfigAdapter {

        protected String component;
        protected String name;
        protected String value;

        @Override
        public Set<String> getComponentNames() {
            return ImmutableSet.of(FOO_COMPONENT);
        }

        @Override
        public void preSetProperty(String componentName, String name, String value) {
            log.info("preSet");
            this.component = componentName;
            this.name = name;
            this.value = value;

        }

        @Override
        public void setProperty(String componentName, String name, String value) {
            log.info("Set");
            this.component = componentName;
            this.name = name;
            this.value = value;

        }
    }

    private class TestComponentContext extends ComponentContextAdapter {

    }

}
