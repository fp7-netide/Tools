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
package eu.netide.gc;

import org.onosproject.net.DeviceId;

/**
 * Services for retrieving MMS informations from the ONOS CLI
 *
 * @author Antonio Marsico (antonio.marsico@create-net.org)
 */
public interface GCServices {

    /**
     * Query the database in order to get the current number of flows inside the DB
     *
     * @return an Integer with the current DB size
     */
    Integer getdbSizeFromCLI(DeviceId id);

}
