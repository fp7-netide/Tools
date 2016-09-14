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
package eu.netide.mms.store;

import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;

import java.util.List;

public interface MMSStoreEntry extends FlowRule, Comparable<MMSStoreEntry> {

    /**
     * Get all the traffic samples received from the network
     * @return a list
     */
    List<Long> packets();

    /**
     * Set the parent flow rules of the current instance
     * @param flowIds a list of flow rule to set
     */
    void setRuleParents(List<MMSStoreEntry> flowIds);

    /**
     * Get all the parent rules of the current instance
     * @return a list of {@link FlowRule}
     */
    List<MMSStoreEntry> getRuleParents();

    /**
     * Utility method to reset the traffic statistics
     * when the rules is reinstalled
     */
    void resetLastPacket();

    /**
     * Get the Exponential Weighted Moving Average calculated for the current
     * instance
     * @return a double
     */
    Double average();

    /**
     * Calculate the EWMA for the current instance
     */
    void calculateExponentialWeightedAverage();

    /**
     * Check the match between the current instance and another rule
     * @param other the other {@link FlowRule}
     * @return a boolean
     */
    boolean checkFlowMatch(FlowRule other);

    /**
     * Add a new packet sample to the instance
     * @param packets the packet sample
     */
    void addPackets (long packets);

    /**
     * Calculate if the current instance is child of another rule
     * @param possibleParent the possible parent {@link FlowRule}
     * @return a boolean
     */
    boolean isParent(FlowRule possibleParent);

    /**
     * Intersect this rules and another one
     * @param possibleParent
     * @return a {@link TrafficSelector} with the result of intersection
     */
    TrafficSelector intersect(FlowRule possibleParent);

    TrafficSelector diff(FlowRule possibleParent);

    boolean getVisited();

    void setVisited(boolean visited);

}