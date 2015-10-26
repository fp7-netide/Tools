package eu.netide.gc.store;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.slf4j.Logger;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

public class DefaultMMSEntry extends DefaultFlowRule implements MMSStoreEntry {

    private long packets;
    private MMSEntryState state;

    private static final Logger log = getLogger(DefaultMMSEntry.class);

    public DefaultMMSEntry(FlowRule rule) {
        super(rule);
        this.packets = 0;
        this.state = MMSEntryState.ACTIVE;
    }

    @Override
    public MMSEntryState state() {

        return this.state;
    }

    @Override
    public long packets() {

        return this.packets;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (packets ^ (packets >>> 32));
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean checkFlowMatch(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMMSEntry) {
            DefaultMMSEntry other = (DefaultMMSEntry) obj;
            if (Objects.equals(this.deviceId(), other.deviceId())) {

                //First: delete all Criteria that the DB entry does not have (Wildcarded tuples)
                //and create a new Set<Criteria> with the TrafficSelector generated from PacketContext

                Set<Criterion> criteriaToCheck = this.selector().criteria().stream()
                        .map(criterion -> other.selector().getCriterion(criterion.type())).collect(Collectors.toSet());

                //Create our local copy of DB TrafficSelector criteria
                Set<Criterion> mmsDBCriteria = ImmutableSet.copyOf(this.selector().criteria());

                //L3 check -> check prefix and address compatibility

                IPCriterion ipDst = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_DST);
                IPCriterion ipSrc = (IPCriterion) this.selector().getCriterion(Criterion.Type.IPV4_SRC);


                boolean ipCompatibility = true;

                if (ipSrc != null && ipDst != null) {

                    IpPrefix otherSrcPrefix = (IpPrefix) ((IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_SRC)).ip();
                    IpPrefix otherDstPrefix = (IpPrefix) ((IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_DST)).ip();

                    ipCompatibility = ipSrc.ip().contains(otherSrcPrefix) && ipDst.ip().contains(otherDstPrefix);

                    mmsDBCriteria.remove(ipDst);
                    mmsDBCriteria.remove(ipSrc);

                    criteriaToCheck.remove(otherSrcPrefix);
                    criteriaToCheck.remove(otherDstPrefix);

                } else if (ipSrc != null && ipDst == null) {

                    IpPrefix otherSrcPrefix = (IpPrefix) ((IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_SRC)).ip();

                    ipCompatibility = ipSrc.ip().contains(otherSrcPrefix);

                    mmsDBCriteria.remove(ipSrc);
                    criteriaToCheck.remove(otherSrcPrefix);

                } else if (ipSrc == null && ipDst != null) {

                    IpPrefix otherDstPrefix = (IpPrefix) ((IPCriterion) other.selector().getCriterion(Criterion.Type.IPV4_DST)).ip();

                    ipCompatibility = ipDst.ip().contains(otherDstPrefix);

                    mmsDBCriteria.remove(ipDst);
                    criteriaToCheck.remove(otherDstPrefix);

                }

                //We can exit if there is no IP compatibility, there will not be any other match
                if (!ipCompatibility)
                    return ipCompatibility;
                else
                    return criteriaToCheck.equals(mmsDBCriteria);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("rule", super.toString())
                .add("state", state).add("packets", packets).toString();
    }

}
