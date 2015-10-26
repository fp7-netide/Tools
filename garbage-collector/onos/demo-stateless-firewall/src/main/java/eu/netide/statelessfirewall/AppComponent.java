/*
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 * Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 * Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.statelessfirewall;

import static org.slf4j.LoggerFactory.getLogger;

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
import org.onlab.packet.TpPort;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo of a stateless firewall.
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
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ApplicationId appId;

    private final Logger log = LoggerFactory.getLogger(getClass());

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
        appId = coreService.registerApplication("eu.netide.statelessfirewall");
        installFirewallRules();

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    protected void deactivate() {

        cfgService.unregisterProperties(getClass(), false);
        //flowRuleService.removeFlowRulesById(appId);
        log.info("Stopped");
    }

    private void installFirewallRules() {


        //World traffic to network
        TrafficSelector.Builder selectorBuilderGeneral = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentGeneral = DefaultTrafficTreatment.builder();

        selectorBuilderGeneral.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(netAddress);

        treatmentGeneral.setOutput(exitPortNet);

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilderGeneral.build())
                .withTreatment(treatmentGeneral.build())
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .add();
        flowObjectiveService.forward(edgeRouter,
                                     forwardingObjective);

        //Output traffic from network

        selectorBuilderGeneral = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(netAddress);

        treatmentGeneral = DefaultTrafficTreatment.builder()
                .setOutput(exitPortWorld);

        forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilderGeneral.build())
                .withTreatment(treatmentGeneral.build())
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .add();
        flowObjectiveService.forward(edgeRouter,
                                     forwardingObjective);

        //World traffic to WWW 80

        selectorBuilderGeneral = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(wwwAddress)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(80));

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
