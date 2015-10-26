package eu.netide.gc.store;

import org.onosproject.net.flow.FlowRule;

public interface MMSStoreEntry extends FlowRule {

    enum MMSEntryState {

        SWAPPED,

        ACTIVE

    }

    MMSEntryState state();

    long packets();

    boolean checkFlowMatch(Object obj);

}