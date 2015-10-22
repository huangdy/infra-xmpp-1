package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt.InterestGrpManagementEventFactory;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.InterestGrpMgmtEventExtension;

public class InterestGrpMgmtEventListener
    implements PacketListener {

    private Logger log = Logger.getLogger(this.getClass());

    private InterestGroupManager interestGroupManager;

    public InterestGrpMgmtEventListener(InterestGroupManager instance) {

        interestGroupManager = instance;
    }

    /*
     * This particular processPacket is only called on a core that owns an interest group when a
     * joined core is requesting an update. (non-Javadoc)
     * 
     * 
     * @see
     * org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware.smack.packet.Packet)
     */
    public void processPacket(Packet packet) {

        // log.debug("IncdMgmtEventListener "+packet.toXML());
        if (packet instanceof Message) {
            PacketExtension ext = packet.getExtension(InterestGrpManagementEventFactory.NAMESPACE);
            if (ext != null && ext instanceof InterestGrpMgmtEventExtension) {
                InterestGrpMgmtEventExtension event = (InterestGrpMgmtEventExtension) ext;

                // Get the Interest group for the interestGroupID
                String interestGroupID = event.getUuid();

                // Push the content to the topic
                // log.debug("IncdMgmtEventListener pushing update request to ");
                if (interestGroupManager.isInterestGroupOwned(interestGroupID)) {
                    synchronized (interestGroupManager.getProcessSuspendedUpdatesLock()) {
                        // In case interestGroup gets deleted from when we checked
                        // isInterestGroupOwned
                        // TODO:
                        // processUpdateRequest(interestGroupID, event.getTopic(),
                        // event.getContent());

                    }
                } else {
                    log.error("IncdMgmtEventListener received a message for an unowned interest group: " +
                              packet.toXML());
                }
            }
        }
    }
}
