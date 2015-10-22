package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.IQ;

import com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestGroupManager.CORE_STATUS;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory;

public class InterestGroup {

    private Logger log = Logger.getLogger(this.getClass());

    public String interestGroupID;
    public String interestGroupType;
    public String interestGroupOwner;
    public String interestGroupPubsubService;
    public String interestGroupNode;

    public List<String> workProductTypes = new ArrayList<String>();
    public Properties ownerProps;
    public CORE_STATUS state;
    public String interestGroupInfo;
    public boolean suspendUpdateProcessing;

    // key: joinedCore
    // value: sharedAllTypes
    public HashMap<String, Boolean> joinedCoreJIDMap = new HashMap<String, Boolean>();

    /**
     * Suspend any requests for updates. Typically used while a new core is joining the interest
     * group.
     */
    public void suspendUpdateProcessing(String interestGroupID) {

        suspendUpdateProcessing = true;
    }

    /**
     * Stop suspending requests for updates.
     */
    public void resetSuspendUpdateProcessing(String interestGroupID) {

        suspendUpdateProcessing = false;
    }

    /**
     * Process any commands, in order, that were put in suspended list while a core was joining.
     */
    public void processSuspendedUpdates() {

        // Iterator<SuspendedCommand> it = suspendedUpdates.iterator();
        // while (it.hasNext()) {
        // it.next().execute();
        // }

        // Clear the suspended commands list
        // suspendedUpdates.clear();
    }

    public boolean addCoreToInterestGroupAfterJoin(String interestGroupID, String coreName) {

        boolean added = true;

        // TODO: what is it doing here!
        // Remove from joining cores list
        // removeJoiningCore(coreName, interestGroupID);

        // addCoreToInterest group(interestGroupID, coreName);

        return added;
    }

}
