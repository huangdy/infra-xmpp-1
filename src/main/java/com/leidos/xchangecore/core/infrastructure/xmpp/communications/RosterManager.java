package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidos.xchangecore.core.infrastructure.messages.CoreStatusUpdateMessage;

public class RosterManager {

    private class CoreRosterListener
        implements RosterListener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final CoreConnection coreConnection;

        public CoreRosterListener(CoreConnection coreConnection) {

            this.coreConnection = coreConnection;
        }

        @Override
        public void entriesAdded(Collection<String> jids) {

            for (String jid : jids) {

                logger.debug("entriesAdded: [" + jid + "]: avialability: " +
                             roster.getPresence(jid).getType() + ", subscription: " +
                             roster.getEntry(jid).getType());
                rosterSubscriptionMap.put(jid, roster.getEntry(jid).getType());
                // rosterMap.put(jid, jid);
                coreConnection.sendCoreStatusUpdate(jid,
                    roster.getEntry(jid).getType().equals(ItemType.both)
                                                                        ? CoreStatusUpdateMessage.Status_Available
                                                                        : CoreStatusUpdateMessage.Status_UnAvailable,
                    "",
                    "");
            }
        }

        @Override
        public void entriesDeleted(Collection<String> jids) {

            for (String jid : jids) {
                logger.info("entriesDeleted: [" + jid + "]");
            }
        }

        @Override
        public synchronized void entriesUpdated(Collection<String> jids) {

            logger.debug("entriesUpdated: ... start ...");

            for (String jid : jids) {

                ItemType subscription = roster.getEntry(jid).getType();
                logger.debug("entriesUpdated: [" + jid + "]: availability: " +
                             roster.getPresence(jid).getType() + ", subscription: " + subscription +
                             ", previouse subscription: " + rosterSubscriptionMap.get(jid));
                String lat = "";
                String lon = "";
                if (rosterLatLonMap.get(jid) != null) {
                    logger.debug("entriesUpdated: lat,lon " + rosterLatLonMap.get(jid));
                    String[] tokens = rosterLatLonMap.get(jid).split(",", -1);
                    lat = tokens[0];
                    lon = tokens[1];
                }

                logger.debug("entriesUpdated: set [" +
                             jid +
                             "] to " +
                             (subscription.equals(ItemType.both)
                                                                ? CoreStatusUpdateMessage.Status_Subscribed
                                                                : CoreStatusUpdateMessage.Status_UnAvailable));
                if (subscription.equals(ItemType.none) && !rosterMap.containsKey(jid)) {
                    removeRoster(jid);
                    coreConnection.sendCoreStatusUpdate(jid,
                        CoreStatusUpdateMessage.Status_UnSubscribed,
                        lat,
                        lon);
                } else if (subscription.equals(ItemType.both)) {
                    logger.debug("entriesUpdated: status is both, set [" + jid +
                                 "] to true for mutually agreed");
                    coreConnection.setRemoteCoreMutuallyAgreed(jid, true);
                    coreConnection.sendCoreStatusUpdate(jid,
                        CoreStatusUpdateMessage.Status_Available,
                        lat,
                        lon);
                } else {
                    if (subscription.equals(ItemType.from) && rosterMap.containsKey(jid)) {
                        logger.debug("entriesUpdated: " + jid + " is online, re-subscribe it");
                        sendSubscribeRequest(jid);
                    }
                    coreConnection.sendCoreStatusUpdate(jid,
                        CoreStatusUpdateMessage.Status_UnAvailable,
                        lat,
                        lon);
                    // getAgreementDAO().setRemoteCoreMutuallyAgreed(jid, false);
                }
                logger.debug("entriesUpdated: save [" + subscription + "] as " + jid +
                             " as previous subscription");
                rosterSubscriptionMap.put(jid, subscription);
                /*
                 * if (subscription.equals(ItemType.none)) { if
                 * (!rosterSubscriptionMap.get(jid).equals(ItemType.none)) {
                 * logger.debug("entriesUpdated: remove " + jid +
                 * " from roster since previous subscription is " + rosterSubscriptionMap.get(jid));
                 * removeRoster(jid); coreConnection.sendCoreStatusUpdate(jid,
                 * CoreStatusUpdateMessage.Status_UnSubscribed, lat, lon); } } else if
                 * (subscription.equals(ItemType.from) || subscription.equals(ItemType.to)) { if
                 * (subscription.equals(ItemType.from) &&
                 * rosterSubscriptionMap.get(jid).equals(ItemType.none) &&
                 * rosterMap.containsKey(jid)) { logger.debug("entriesUpdated: " + jid +
                 * " is online, re-subscribe it"); sendSubscribeRequest(jid); }
                 * logger.debug("entriesUpdated: set " + jid + " to offline");
                 * coreConnection.sendCoreStatusUpdate(jid,
                 * CoreStatusUpdateMessage.Status_UnSubscribed, lat, lon); } else {
                 * logger.debug("entriesUpdated: set " + jid + " to online");
                 * coreConnection.sendCoreStatusUpdate(jid,
                 * CoreStatusUpdateMessage.Status_Available, lat, lon); }
                 */
            }

            logger.debug("entriesUpdated: ... done ...");
        }

        @Override
        public void presenceChanged(Presence presence) {

            String remoteJID = presence.getFrom();
            ItemType subscription = roster.getEntry(presence.getTo()).getType();

            logger.debug("presenceChanged: fromJID: " + remoteJID + ", presence: " +
                         presence.getType() + ", toJID: " + presence.getTo() + ",  suscription: " +
                         subscription);
            logger.debug("presenceChanged: [" + presence.toXML() + "]");

            String availability = CoreStatusUpdateMessage.Status_UnSubscribed;
            if (presence.getType().equals(Type.available) && subscription.equals(ItemType.both)) {
                availability = CoreStatusUpdateMessage.Status_Available;
            }
            DefaultPacketExtension geoloc = (DefaultPacketExtension) presence.getExtension("geoloc",
                "http://jabber.org/protocol/geoloc");
            if (geoloc == null) {
                coreConnection.sendCoreStatusUpdate(remoteJID, availability, "", "");
            } else {
                coreConnection.sendCoreStatusUpdate(remoteJID,
                    availability,
                    geoloc.getValue("lat"),
                    geoloc.getValue("lon"));
            }
        }
    }

    private class PresencePacketListener
        implements PacketListener {

        private final Logger logger = LoggerFactory.getLogger(PresencePacketListener.class);

        @Override
        public void processPacket(Packet packet) {

            // Ignore errors
            if (packet.getError() != null) {
                return;
            }

            // logger.debug("processPacket: " + packet.toXML());

            if (packet instanceof Presence) {

                Presence presence = (Presence) packet;
                // Get the JID
                String bareJID = org.jivesoftware.smack.util.StringUtils.parseBareAddress(presence.getFrom());
                String fullJID = presence.getFrom();

                // Ignore presence messages from this core (all resources)
                if (!bareJID.equalsIgnoreCase(coreConnection.getJID())) {

                    DefaultPacketExtension geoloc = (DefaultPacketExtension) presence.getExtension("geoloc",
                        "http://jabber.org/protocol/geoloc");

                    // Check for any outstanding presence subscription requests and
                    // respond if necessary and it is in our list of knownCores
                    if (rosterMap.containsKey(bareJID.toLowerCase())) {
                        logger.debug("processPacket: [" + fullJID + "], availability: " +
                                     roster.getPresence(fullJID).getType() + ", subscription: " +
                                     roster.getEntry(fullJID).getType() + ", presenceType: " +
                                     presence.getType());
                        if (geoloc != null) {
                            logger.debug("processPacket: lat/lon: " + geoloc.getValue("lat") + "/" +
                                         geoloc.getValue("lon"));
                            rosterLatLonMap.put(fullJID, new String(geoloc.getValue("lat") + "," +
                                                                    geoloc.getValue("lon")));
                            coreConnection.sendCoreStatusUpdate(fullJID,
                                roster.getEntry(fullJID).getType().equals(ItemType.both)
                                                                                        ? CoreStatusUpdateMessage.Status_Available
                                                                                        : CoreStatusUpdateMessage.Status_UnAvailable,
                                geoloc.getValue("lat"),
                                geoloc.getValue("lon"));
                        }
                    }
                }
            }
        }
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Key is JID of core, value is the title for the core
    private static HashMap<String, String> rosterMap = new HashMap<String, String>();
    private static HashMap<String, ItemType> rosterSubscriptionMap = new HashMap<String, ItemType>();
    private static HashMap<String, String> rosterLatLonMap = new HashMap<String, String>();

    // TODO: look at if the knownCores is still needed. Can probably just use the rosterMap keys
    private CoreConnection coreConnection;
    private Roster roster;

    public RosterManager() {

    }

    /**
     * Constructor
     *
     * @param con
     *            - CoreConnection to use
     */
    public RosterManager(CoreConnection con) {

        coreConnection = con;

        if (!rosterMap.containsValue(coreConnection.getServer().toLowerCase())) {
            logger.debug("RosterManager : adding to map: JID=" + coreConnection.getJID() +
                         " name=" + coreConnection.getServer());
            rosterMap.put(coreConnection.getJID(), coreConnection.getJID());
        }

        // Get the initial roster
        roster = coreConnection.getXmppConnection().getRoster();

        // Check the roster against the configured known cores if configured
        // also don't mess with the owners roster
        if (roster != null) {
            printRoster();
            updateRosterMap();
        }

        // Add a roster listener
        roster.addRosterListener(new CoreRosterListener(con));

        // Set subscription mode to accept all automatically
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        // add presence listener
        coreConnection.addPacketListener(new PresencePacketListener(),
            new PacketTypeFilter(Presence.class));
    }

    public ItemType createEntry(String remoteJID, String name, String[] groups) {

        logger.debug("createEntry: adding to map JID: " + remoteJID + ", name: " + name);
        rosterMap.put(remoteJID, name);

        if (!remoteJID.equalsIgnoreCase(coreConnection.getJID())) {
            try {
                if (!roster.contains(remoteJID)) {
                    logger.debug("createEntry: [" + remoteJID +
                                 "] not existed in roster, create it");
                    roster.createEntry(remoteJID, name, groups);
                }
                logger.debug("createEntry: subscribe to " + remoteJID);
                sendSubscribeRequest(remoteJID);
                rosterSubscriptionMap.put(remoteJID, ItemType.none);
                return roster.getEntry(remoteJID).getType();
            } catch (Exception e) {
                logger.error("createEntry: " + e.getMessage());
            }
        }
        return ItemType.none;
    }

    private String getHostNameFromJID(String jid) {

        String hostName = null;
        int pos = jid.indexOf("@");
        if (pos > 0) {
            hostName = jid.substring(pos + 1);
        }
        return hostName;
    }

    /**
     * Get the name for a core given the JID
     *
     * @param coreJID
     *            interestmanager@server
     * @return
     */
    public String getJIDFromRosterName(String coreName) {

        logger.debug("getJIDFromRosterName for " + coreName);
        return coreName; // rosterMap.inverse().get(coreName);
    }

    /**
     * Get a Map of the rosters keyed by roster name
     *
     * @return Map with core name as key and core JID as value.
     */
    public Map<String, String> getRosterByName() {

        return rosterMap;
    }

    public String getRosterNameFromJID(String coreJID) {

        return rosterMap.get(coreJID.toLowerCase());
    }

    /**
     * Get a Map of all the cores in the roster and their status.
     *
     * @return Map with core name as key and presence as value.
     */
    public Map<String, String> getRosterStatus() {

        HashMap<String, String> map = new HashMap<String, String>();
        Collection<RosterEntry> rosters = roster.getEntries();
        for (RosterEntry entry : rosters) {
            logger.debug("getRosterStatus: JID: " +
                         entry.getUser() +
                         ", availabilty: " +
                         (roster.getEntry(entry.getUser()).getType().equals(ItemType.both)
                                                                                          ? CoreStatusUpdateMessage.Status_Available
                                                                                          : CoreStatusUpdateMessage.Status_UnSubscribed));
            map.put(entry.getUser(),
                roster.getEntry(entry.getUser()).getType().equals(ItemType.both)
                                                                                ? CoreStatusUpdateMessage.Status_Available
                                                                                : CoreStatusUpdateMessage.Status_UnSubscribed);
        }
        return map;
    }

    public ItemType getRosterSubscription(String remoteJID) {

        roster.reload();
        return roster.getEntry(remoteJID).getType();
    }

    /**
     * Check if the roster contains the given core. The input core name must be the exact name (case
     * sensitive) that was set in the XMPP roster that was set in the roster config service. If
     * multiple cores use different names for a core this method will fail if looking up a core name
     * from a different machine.
     *
     * @param coreName
     *            the roster entry name for the core
     * @return true if the core is in the roster
     */
    public boolean isCoreInRoster(String coreName) {

        // get JID from core name
        String jid = getJIDFromRosterName(coreName);
        logger.info("isCoreInRoster: JID=[" + jid + "]");
        return jid != null ? roster.contains(jid) : false;
    }

    /**
     * Check if the given core is online. See limitations on the core name in the isCoreInRoster
     * comments.
     *
     * @param coreName
     *            the roster entry name for the core
     * @return true if the core is available (online)
     */
    public boolean isCoreOnline(String coreName) {

        return roster.getEntry(coreName).getType().equals(ItemType.both) ? true : false;
    }

    /**
     * Print the roster
     */
    public void printRoster() {

        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            logger.debug("printRoster: JID: " + entry.getUser() + ", availability: " +
                         roster.getPresence(entry.getUser()).getType() + ", subscription: " +
                         entry.getType());
        }
    }

    public void removeEntry(String remoteJID) {

        logger.info("removeEntry: JID: " + remoteJID);
        if (rosterMap.containsKey(remoteJID)) {
            rosterMap.remove(remoteJID);
        }
        if (rosterSubscriptionMap.get(remoteJID).equals(ItemType.none)) {
            coreConnection.sendCoreStatusUpdate(remoteJID,
                CoreStatusUpdateMessage.Status_UnSubscribed,
                "",
                "");
        } else {
            sendUnsubscribeRequest(remoteJID);
        }
    }

    private void removeRoster(String jid) {

        logger.debug("removeRoster: " + jid);

        if (rosterMap.containsKey(jid)) {
            rosterMap.remove(jid);
        }
        RosterEntry entry = roster.getEntry(jid);
        logger.debug("removeRoser: entry: " + (entry == null ? "not" : entry.getUser()) + " found");
        if (entry == null) {
            return;
        }
        try {
            roster.removeEntry(entry);
        } catch (XMPPException e) {
            logger.error("removeRoster: " + jid + ": " + e.getMessage());
        }
    }

    protected void sendSubscribeRequest(String remoteJID) {

        logger.info("sendSubscribeRequest: to: [" + remoteJID + "]");
        Presence p = new Presence(Presence.Type.subscribe);
        p.setTo(remoteJID);
        try {
            coreConnection.sendPacketCheckWellFormed(p);
        } catch (XMPPException e) {
            logger.error("Error sending subscribe request: " + e.getMessage());
            logger.error(p.toXML());
        }
        roster.reload();
    }

    private void sendUnsubscribeRequest(String core) {

        logger.info("sendUnsubscribeRequest: to: [" + core + "]");
        Presence p = new Presence(Presence.Type.unsubscribe);
        p.setTo(core);
        try {
            coreConnection.sendPacketCheckWellFormed(p);
        } catch (XMPPException e) {
            logger.error("sendUnsubscribeRequest: " + e.getMessage());
            logger.error(p.toXML());
        }
        roster.reload();
    }

    public void unSubscribeAll() {

        Collection<RosterEntry> rosters = roster.getEntries();
        for (RosterEntry roster : rosters) {
            logger.debug("unSubscribeAll: unsubscribe: " + roster.getUser());
            sendUnsubscribeRequest(roster.getUser());
        }
    }

    private void updateRosterMap() {

        Collection<RosterEntry> rosters = roster.getEntries();
        for (RosterEntry rosterEntry : rosters) {
            logger.debug("updateRosterMap: [" + rosterEntry.getUser() + " with subscription: " +
                         rosterEntry.getType());
            if (rosterEntry.getType().equals(ItemType.none)) {
                try {
                    roster.removeEntry(rosterEntry);
                } catch (Exception e) {
                    logger.warn("updateRosterEntry: [" + rosterEntry.getUser() + "]: " +
                                e.getMessage());
                }
            } else {
                rosterMap.put(rosterEntry.getUser(), rosterEntry.getName());
                rosterSubscriptionMap.put(rosterEntry.getUser(), rosterEntry.getType());
            }
        }
    }
}