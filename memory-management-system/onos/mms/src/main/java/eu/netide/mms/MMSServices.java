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

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Services for retrieving MMS informations from the ONOS CLI
 *
 * @author Antonio Marsico (antonio.marsico@create-net.org)
 */
public interface MMSServices {

    /**
     * Query the database of swapped flows in order to get the current size
     *
     * @return an Integer with the current DB size
     */
    Integer getdbSizeFromCLI(DeviceId id);

    /**
     * Method to add an ONOS application to the MMS deallocation function
     *
     * @param app The {@link ApplicationId} to add
     */
    void addAppToMMS(ApplicationId app);

    /**
     * Remove an application from MMS deallocation function
     *
     * @param app The {@link ApplicationId} to remove
     */
    void deleteAppFromMMS(ApplicationId app);

    /**
     * Get the applications already under the control of the MMS
     *
     * @return a {@link Set} of {@link ApplicationId}
     */
    Set<ApplicationId> getApplicationsMMS();
}
