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
package eu.netide.mms.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.intent.constraint.BooleanConstraint;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

public class DefaultMMSEntry extends DefaultFlowRule implements MMSStoreEntry, Comparable<MMSStoreEntry> {

    private List<Long> packets;
    private Double average;
    private Long lastPacket;
    private List<MMSStoreEntry> ruleParents;
    private boolean visited;

    private static final Logger log = getLogger(DefaultMMSEntry.class);

    public DefaultMMSEntry(FlowRule rule) {
        super(rule);
        this.packets = Lists.newArrayList();
        this.average = 0.0;
        this.lastPacket = (long) 0;
        this.ruleParents = Lists.newArrayList();
    }

    public DefaultMMSEntry(FlowEntry rule) {
        super(rule);
        this.packets = Lists.newArrayList(rule.packets());
        this.average = 0.0;
        this.lastPacket = (long) 0;
        this.ruleParents = Lists.newArrayList();
    }

    public DefaultMMSEntry(MMSStoreEntry rule) {
        super(rule);
        this.packets = rule.packets();
        this.average = rule.average();
        this.lastPacket = (long) 0;
        this.ruleParents = rule.getRuleParents();
    }

    @Override
    public List<Long> packets() {

        return this.packets;
    }

    @Override
    public void setRuleParents(List<MMSStoreEntry> parentRules) {
        this.ruleParents = parentRules;
    }

    @Override
    public List<MMSStoreEntry> getRuleParents() {
        return this.ruleParents;
    }

    @Override
    public void resetLastPacket() {
        this.lastPacket = (long) 0;
    }

    @Override
    public Double average() {
        return this.average;
    }

    @Override
    public boolean getVisited() {
        return this.visited;
    }

    @Override
    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    @Override
    public void calculateExponentialWeightedAverage() {

        ExponentialMovingAverage averageCalculator = new ExponentialMovingAverage(0.5);

        List<Long> packetsHistory = this.packets;

        double calculatedAverage = 0;

        for (int i = 0; i < packets.size(); i++) {

            calculatedAverage = averageCalculator.average(packetsHistory.get(i));
        }

        this.average = calculatedAverage;
    }

    private class ExponentialMovingAverage {
        private double alpha;
        private Double oldValue;

        public ExponentialMovingAverage(double alpha) {
            this.alpha = alpha;
        }

        public double average(double value) {
            if (oldValue == null) {
                oldValue = value;
                return value;
            }
            double newValue = oldValue + alpha * (value - oldValue);
            oldValue = newValue;
            return newValue;
        }
    }


    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + packets.hashCode();
        result = 31 * result + average.hashCode();
        result = 31 * result + lastPacket.hashCode();
        return result;
    }

    @Override
    public boolean checkFlowMatch(FlowRule other) {

        //this -> the rule inside the DB
        //other -> the PacketContext arrived

        //First: delete all Criteria that the DB entry does not have (Wildcarded tuples)
        //and create a new Set<Criteria> with the TrafficSelector generated from PacketContext

        Set<Criterion> criteriaToCheck = this.selector().criteria().stream()
                .map(criterion -> other.selector().getCriterion(criterion.type())).collect(Collectors.toSet());

        //Create our local copy of DB TrafficSelector criteria
        Set<Criterion> mmsDBCriteria = Sets.newHashSet(this.selector().criteria());

        //L3 check -> check prefix and address compatibility

        IPCriterion ipDst = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_DST);
        IPCriterion ipSrc = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_SRC);


        boolean ipCompatibility = true;

        if (ipSrc != null && ipDst != null) {

            IPCriterion otherSrcIP = (IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_SRC);
            IPCriterion otherDstIP = (IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_DST);

            IpPrefix otherSrcPrefix = otherSrcIP.ip();
            IpPrefix otherDstPrefix = otherDstIP.ip();

            ipCompatibility = ipSrc.ip().contains(otherSrcPrefix) && ipDst.ip().contains(otherDstPrefix);

            mmsDBCriteria.remove(ipDst);
            mmsDBCriteria.remove(ipSrc);

            criteriaToCheck.remove(otherSrcIP);
            criteriaToCheck.remove(otherDstIP);

        } else if (ipSrc != null && ipDst == null) {

            IPCriterion otherSrcIP = (IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_SRC);

            IpPrefix otherSrcPrefix = otherSrcIP.ip();

            ipCompatibility = ipSrc.ip().contains(otherSrcPrefix);

            mmsDBCriteria.remove(ipSrc);
            criteriaToCheck.remove(otherSrcIP);

        } else if (ipSrc == null && ipDst != null) {

            IPCriterion otherDstIP = (IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_DST);

            IpPrefix otherDstPrefix = otherDstIP.ip();

            ipCompatibility = ipDst.ip().contains(otherDstPrefix);

            mmsDBCriteria.remove(ipDst);
            criteriaToCheck.remove(otherDstIP);

        }

        //We can exit if there is no IP compatibility, there will not be any other match
        if (!ipCompatibility)
            return ipCompatibility;
        else
            return criteriaToCheck.equals(mmsDBCriteria);
    }

    @Override
    public boolean isParent(FlowRule possibleParent) {

        //this -> the rule inside the DB
        //possibleParent -> the possible parent of this rule
        //If the child priority is less than parent, there is no dependency
        if (this.priority() < possibleParent.priority()) {
            return false;
        }

        //First: delete all Criteria that the DB entry does not have (Wildcarded tuples)
        //and create a new Set<Criteria> with the TrafficSelector generated from PacketContext
        Set<Criterion> possibleParentCriteria =
                Sets.newHashSet(possibleParent.selector().criteria());

        //Create our local copy of DB TrafficSelector criteria
        Set<Criterion> mmsDBCriteria = Sets.newHashSet(this.selector().criteria());

        //L3 check -> check prefix and address compatibility

        IPCriterion ipDst = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_DST);
        IPCriterion ipSrc = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_SRC);

        IPCriterion otherSrcIP = (IPCriterion) possibleParent.selector().getCriterion(Criterion.Type.IPV4_SRC);
        IPCriterion otherDstIP = (IPCriterion) possibleParent.selector().getCriterion(Criterion.Type.IPV4_DST);

        boolean ipCompatibility = true;

        if ((ipSrc != null && ipDst != null) && (otherSrcIP != null && otherDstIP != null)) {

            IpPrefix otherSrcPrefix = otherSrcIP.ip();
            IpPrefix otherDstPrefix = otherDstIP.ip();

            ipCompatibility = (ipSrc.ip().contains(otherSrcPrefix) && ipDst.ip().contains(otherDstPrefix))
                    || (otherDstPrefix.contains(ipDst.ip()) && otherSrcPrefix.contains(ipSrc.ip()));

            mmsDBCriteria.remove(ipDst);
            mmsDBCriteria.remove(ipSrc);

            possibleParentCriteria.remove(otherSrcIP);
            possibleParentCriteria.remove(otherDstIP);

        } else if ((ipSrc != null && ipDst == null) && (otherSrcIP != null && otherDstIP == null)) {

            IpPrefix otherSrcPrefix = otherSrcIP.ip();

            ipCompatibility = ipSrc.ip().contains(otherSrcPrefix) || otherSrcPrefix.contains(ipSrc.ip());

            mmsDBCriteria.remove(ipSrc);
            possibleParentCriteria.remove(otherSrcIP);

        } else if ((ipSrc == null && ipDst != null) && (otherSrcIP == null && otherDstIP != null)) {

            IpPrefix otherDstPrefix = otherDstIP.ip();

            ipCompatibility = ipDst.ip().contains(otherDstPrefix) || otherDstPrefix.contains(ipDst.ip());

            mmsDBCriteria.remove(ipDst);
            possibleParentCriteria.remove(otherDstIP);

        }

        //We can exit if there is no IP compatibility, there will not be any other match
        if (!ipCompatibility) {
            return ipCompatibility;
        } else {

            //Since we do not have a Wilcard criteria, we have to check every value
            boolean notIntersects = mmsDBCriteria.stream().anyMatch(
                    criterion -> {
                        Criterion criterionToCheck = possibleParent.selector().getCriterion(criterion.type());

                        //If the criterion is null, we have a wildcard, so the rule potentially intersects
                        if (criterionToCheck != null) {
                            if (!criterionToCheck.equals(criterion)) {
                                //If only a value is different, the rules do not intersect
                                return true;
                            }
                        }

                        return false;
                    }
            );
            //Since anyMatch returns true if there are no intersections, we negate the value
            return !notIntersects;
        }
    }

    @Override
    public TrafficSelector intersect(FlowRule possibleParent) {

        //this -> the rule inside the DB
        //possibleParent -> the possible parent of this rule
        //If the child priority is less than parent, there is no dependency
        if (this.priority() < possibleParent.priority()) {
            return DefaultTrafficSelector.emptySelector();
        }

        Set<Criterion> possibleParentCriteria =
                Sets.newHashSet(possibleParent.selector().criteria());

        //Create our local copy of DB TrafficSelector criteria
        Set<Criterion> mmsDBCriteria = Sets.newHashSet(this.selector().criteria());

        //L3 check -> check prefix and address compatibility

        IPCriterion ipDst = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_DST);
        IPCriterion ipSrc = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_SRC);

        IPCriterion otherSrcIP = (IPCriterion) possibleParent.selector().getCriterion(Criterion.Type.IPV4_SRC);
        IPCriterion otherDstIP = (IPCriterion) possibleParent.selector().getCriterion(Criterion.Type.IPV4_DST);

        boolean childContainsParent = true;

        //We have to check IP TUPLES
        if (ipSrc != null && otherSrcIP != null) {

            IpPrefix otherSrcPrefix = otherSrcIP.ip();

            //IP intersection, if other rule contains this (e.g. 192.168.0.0/24 contains 192.168.1.1/32), return the other Criterion

            if (ipSrc.ip().contains(otherSrcPrefix)) {

                //Since the other rule "wins", delete our criterion and add the other
                mmsDBCriteria.remove(ipSrc);
                mmsDBCriteria.add(otherSrcIP);
                childContainsParent = false;

                possibleParentCriteria.remove(otherSrcIP);


            } else if (otherSrcPrefix.contains(ipSrc.ip())) {
                //Since this rule contains the other, remove from the other our Criterion
                possibleParentCriteria.remove(otherSrcIP);

            } else {
                //The rules do not intersect, return empty selector
                return DefaultTrafficSelector.emptySelector();
            }
        }

        if (ipDst != null && otherDstIP != null) {

            IpPrefix otherDstPrefix = otherDstIP.ip();

            if (ipDst.ip().contains(otherDstPrefix)) {
                mmsDBCriteria.remove(ipDst);
                mmsDBCriteria.add(otherDstIP);
                childContainsParent = false;

                possibleParentCriteria.remove(otherDstIP);

            } else if (otherDstPrefix.contains(ipDst.ip())) {
                possibleParentCriteria.remove(otherDstIP);
            } else {
                //The rules do not intersect, return empty selector
                return DefaultTrafficSelector.emptySelector();
            }

        }

        if (ipDst == null && otherDstIP != null) {

            mmsDBCriteria.add(otherDstIP);
            possibleParentCriteria.remove(otherDstIP);

        }

/*        if (ipDst != null && otherDstIP == null) {
            //There is one case that the wildcards are in opposite places (e.g. this.IP_SRC and other.IP_DST)
            //In this case, the intersection is the union between the two
        }

        if (ipSrc != null && otherSrcIP == null) {

        } */

        if (ipSrc == null && otherSrcIP != null) {
            mmsDBCriteria.add(otherSrcIP);
            possibleParentCriteria.remove(otherSrcIP);
        }

        //mmsDBCriteria now represent the IP intersection between the two rules

        //Since we do not have a Wilcard criteria, we have to check every value
        boolean notIntersects = mmsDBCriteria.stream().anyMatch(
                criterion -> {
                    for (Criterion criterionToCheck : possibleParentCriteria) {

                        //If the criterion is null, we have a wildcard, so the rule potentially intersects
                        if (criterionToCheck.type() == criterion.type()) {
                            if (!criterionToCheck.equals(criterion)) {
                                //If only a value is different, the rules do not intersect
                                return true;
                            }
                        }
                    }
                    return false;
                }
        );
        //Since anyMatch returns true if there are no intersections, we send the value
        if(notIntersects) {
            return DefaultTrafficSelector.emptySelector();
        } else {
            mmsDBCriteria.addAll(possibleParentCriteria);
        }

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                for(Criterion criteriaToAdd : mmsDBCriteria) {
                    selectorBuilder.add(criteriaToAdd);
                }

        return selectorBuilder.build();
    }

    @Override
    public TrafficSelector diff(FlowRule possibleParent) {

        //this -> the rule inside the DB
        //possibleParent -> the possible parent of this rule
        //If the child priority is less than parent, there is no dependency
        if (this.priority() < possibleParent.priority()) {
            return DefaultTrafficSelector.emptySelector();
        }

        Set<Criterion> possibleParentCriteria =
                Sets.newHashSet(possibleParent.selector().criteria());

        //Create our local copy of DB TrafficSelector criteria
        Set<Criterion> mmsDBCriteria = Sets.newHashSet(this.selector().criteria());

        //L3 check -> check prefix and address compatibility

        IPCriterion ipDst = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_DST);
        IPCriterion ipSrc = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_SRC);

        IPCriterion otherSrcIP = (IPCriterion) possibleParent.selector().getCriterion(Criterion.Type.IPV4_SRC);
        IPCriterion otherDstIP = (IPCriterion) possibleParent.selector().getCriterion(Criterion.Type.IPV4_DST);

        //In the ONOS logic, if parent_ip contains child, means child contains parent in CacheFlow logic
        //This because 192.168.0.0/24 (parent) contains 192.168.1.1/32 (child)
        boolean childContainsParent = true;
        boolean wildcardsOnIP = false;

        //We have to check IP TUPLES
        if (ipSrc != null && otherSrcIP != null) {

            IpPrefix otherSrcPrefix = otherSrcIP.ip();

            //IP intersection, if other rule contains this (e.g. 192.168.0.0/24 contains 192.168.1.1/32), return the other Criterion

            if (otherSrcPrefix.contains(ipSrc.ip())) {
                //Since this rule contains the other, remove from the other our Criterion
                possibleParentCriteria.remove(otherSrcIP);

            } else {
                //The rules do not intersect, return empty selector
                childContainsParent = false;
            }
        }

        if (ipDst != null && otherDstIP != null) {

            IpPrefix otherDstPrefix = otherDstIP.ip();

            if (otherDstPrefix.contains(ipDst.ip())) {
                possibleParentCriteria.remove(otherDstIP);
            } else {
                //The rules do not intersect, return empty selector
                childContainsParent = false;
            }

        }

        if (ipDst == null && otherDstIP != null) {

            //mmsDBCriteria.add(otherDstIP);
            possibleParentCriteria.remove(otherDstIP);
            childContainsParent = false;

        }

        /*if (ipDst != null && otherDstIP == null) {
            //There is one case that the wildcards are in opposite places (e.g. this.IP_SRC and other.IP_DST)
            //In this case, the intersection is the union between the two
        }

        if (ipSrc != null && otherSrcIP == null) {

        }*/

        if (otherSrcIP == null && otherDstIP == null) {
            childContainsParent = false;
        }

        if (ipSrc == null && ipDst == null) {
            childContainsParent = false;
        }

        if (ipSrc == null && otherSrcIP != null) {
            //mmsDBCriteria.add(otherSrcIP);
            possibleParentCriteria.remove(otherSrcIP);
            childContainsParent = false;
        }

        if (ipSrc == null && ipDst == null && otherSrcIP == null && otherDstIP == null) {
            wildcardsOnIP = true;
        }

        if(wildcardsOnIP) {
            if(mmsDBCriteria.containsAll(possibleParentCriteria)) {

                TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                for(Criterion criteriaToAdd : mmsDBCriteria) {
                    selectorBuilder.add(criteriaToAdd);
                }
                return selectorBuilder.build();
            } else {
                //Since we do not have a Wilcard criteria, we have to check every value

                boolean containsAtLeastOne = mmsDBCriteria.stream().anyMatch(
                        criterion -> {
                            //If the criterion is null, we have a wildcard, so the rule potentially intersects
                            return possibleParentCriteria.contains(criterion);
                        }
                );

                if(containsAtLeastOne) {
                    return this.intersect(possibleParent);
                } else {
                    TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                    for(Criterion criteriaToAdd : mmsDBCriteria) {
                        selectorBuilder.add(criteriaToAdd);
                    }
                    return selectorBuilder.build();
                }
            }
        }

        //Now lets check if this Set criteria contains all the other criteria
        if(childContainsParent) {
            //we can enter here in one case, we have the child that contains all the IPs of parent
            if(possibleParentCriteria.isEmpty()) {
                //The parent was composed of IP only, so the child contains everything
                return DefaultTrafficSelector.emptySelector();
            }

            if(mmsDBCriteria.containsAll(possibleParentCriteria)) {

                TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                for(Criterion criteriaToAdd : mmsDBCriteria) {
                    selectorBuilder.add(criteriaToAdd);
                }
                return selectorBuilder.build();
            } else {
                //Since child IP contains parent, we need only to do intersection
                return this.intersect(possibleParent);
            }
        } else {
            //We enter only if the child IP does not contains the parent IP, so check only if we have one common match
            boolean containsAtLeastOne = mmsDBCriteria.stream().anyMatch(
                    criterion -> {
                        //If the criterion is null, we have a wildcard, so the rule potentially intersects
                        return possibleParentCriteria.contains(criterion);
                    }
            );

            if(containsAtLeastOne) {
                return this.intersect(possibleParent);
            } else {
                TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                for(Criterion criteriaToAdd : mmsDBCriteria) {
                    selectorBuilder.add(criteriaToAdd);
                }
                return selectorBuilder.build();
            }
        }
    }

    @Override
    public void addPackets(long packets) {


        Long newPackets = packets;

        Long valueToAdd = newPackets - this.lastPacket;

        this.lastPacket = newPackets;

        this.packets.add(valueToAdd);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("rule", super.toString())
                .add("average", average).toString();
    }

    @Override
    public int compareTo(MMSStoreEntry o) {
        return average.compareTo(o.average());
    }
}
