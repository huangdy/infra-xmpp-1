package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

public interface XmppConnection {

    public boolean isConnected();

    /**
     * Internal method to do the actual XMPP connnection
     */
    public void connect();

    /**
     * Disconnect from the XMPP server.
     */
    public void disconnect();

    /**
     * Send an XMPP packet to the server
     * 
     * @param packet
     */
    public void sendPacket(Packet packet);

    /**
     * Create a CommandWithReply instances to handle sending the XMPP packet and wait for a reply.
     * 
     * @param packet
     * @return
     * @throws XMPPException
     */
    public CommandWithReply createCommandWithReply(Packet packet) throws XMPPException;

    /**
     * Add a packet listener with the given packet filter.
     * 
     * @param listener
     * @param packetFilter
     */
    public void addPacketListener(PacketListener listener, PacketFilter packetFilter);

    /**
     * Remove a previously added packet listener.
     * 
     * @param listener
     */
    public void removePacketListener(PacketListener listener);

    /**
     * Create a packet collector to collect packets returned from the server based on the input
     * filter.
     * 
     * @param packetFilter
     * @return
     */
    public PacketCollector createPacketCollector(PacketFilter packetFilter);

    /**
     * Add a new entry for a core to the XMPP roster.
     * 
     * @param coreJID
     *            Core's XMPP JID
     * @param name
     *            human readable name for the coreServerName
     */
    public void addRosterEntry(String coreJID, String name);

    /**
     * Delete an entry from the roster.
     * 
     * @param coreJID
     *            JID of roster entry to remove
     */
    public void deleteRosterEntry(String coreJID);

    /**
     * Get the JID of this connection.
     * 
     * @return
     */
    public String getJID();

    /**
     * Get the JID of the pubsub service for this connection.
     * 
     * @return
     */
    public String getPubSubSvc();

    /**
     * Set the JID of this connections pubsub service.
     * 
     * @param value
     */
    public void setPubSubSvc(String value);

    /**
     * Get a list of items on a node.
     * 
     * @param node
     *            name of node
     * @return
     */
    public DiscoverItems discoverNodeItems(String node);

    /**
     * Get the XMPP information on a node.
     * 
     * @param node
     *            name of node
     * @return
     */
    public DiscoverInfo discoverNodeInfo(String node);

    public int getWaitTimeInSeconds();
}