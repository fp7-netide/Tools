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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.netide.statelessfirewall.AppComponent;

/**
 * Set of tests of the ONOS application component.
 */
public class AppComponentTest {

    private AppComponent component;

    @Before
    public void setUp() {
        component = new AppComponent();
        component.activate();

    }

    @After
    public void tearDown() {
        component.deactivate();
    }

    @Test
    public void basics() {

    }

}
