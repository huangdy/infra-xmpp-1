package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverItems;

import com.leidos.xchangecore.core.infrastructure.xmpp.communications.NodeManagerImpl.NODE_ITEM_TYPE;

/**
 * The NodeManager manages interactions with an XMPP pubsub service. An instance of a NodeManager
 * should be created for each pubsub service.
 * 
 * It manages the list of currently subscribed nodes and provides interfaces to:
 * <ul>
 * <li>get a list of subscriptions
 * <li>subscribe to a node
 * <li>create a node
 * <li>remove a node
 * <li>retrieve items from a node
 * <li>publish an item to a node
 * <li>remove an item from a node
 * </ul>
 * 
 * @author roger
 * 
 */
public interface NodeManager {

    /**
     * set the JID of the XMPP pubsub service to use
     */
    public void setPubsubService(String pubsubService);

    /**
     * Get the jid of the pubsub service
     * 
     * @return jid of pubsub service
     */
    public String getPubsubService();

    /**
     * @return true if object is initialized and ready to use
     */
    public boolean isInitialized();

    /**
     * This is for subscribing to local XMPP server nodes. It will reuse subscriptions if they are
     * already in place.
     * 
     * @param node
     *            node to subscript to
     * @param listener
     *            listener to receive updates on
     * @return list of items that are currently on the node
     */
    public List<String> subscribeToNode(String node, PacketListener listener) throws XMPPException;

    /**
     * Test if a node is currently subscribed to
     * 
     * 
     * @param node
     * @return true if node has a current subscription
     */
    public boolean isSubscribedTo(String node);

    /**
     * Unsubscribe to a particular node
     * 
     * @param node
     */
    public void unsubscribe(String node);

    /**
     * Unsubscribe from all nodes that are subscribed to by the connections JID.
     * 
     */
    public void unsubscribeAll();

    /**
     * Unsubscribe from all nodes that the JID for the connection is subscribed to for the given
     * interest group UUID.
     * 
     * @param uuid
     *            UUID of the interest group to unsubscribe from
     */
    public void unsubscribeAll(String uuid);

    /**
     * Publish an item to a node
     * 
     * @param nodeName
     * @param itemText
     * @return true if item was published
     */
    public boolean publishToNode(String nodeName, String itemText);

    /**
     * Remove an item from a node
     * 
     * @param nodeName
     * @param itemUUID
     * @return true if item was found and removed
     */
    public boolean removeItem(String nodeName, String itemUUID);

    /**
     * Query the XMPP server to update the list of nodes subscribed to.
     */
    public void updateSubscriptionMap();

    public String getSubscriptionID(String interestGroupNode);

    /**
     * Return a list of JIDs that are subscribed to this node.
     * 
     * @param nodeName
     * @return
     */
    List<String> getSubscribedJIDs(String nodeName);

    /**
     * Get a list of children nodes
     * 
     * @param parentNode
     * @return list of children nodes
     */
    public DiscoverItems getChildrenNodes(String parentNode);

    /**
     * Retrieve an item from a node
     * 
     * @param node
     * @param itemID
     * @return item XML as a string
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public String retrieveNodeItem(String node, String itemID) throws IllegalStateException,
        IllegalArgumentException;

    // TODO: fix concurrency problem with using this call and creating Interest groups
    /**
     * Retrieve all the items on a node
     * 
     * @param node
     * @return list of XML for each item on the node
     * @throws IllegalArgumentException
     */
    public ArrayList<String> getAllNodeItems(String node) throws IllegalArgumentException;

    /**
     * Add a node to a collection node.
     * 
     * @param folder
     *            parent node
     * @param topic
     *            name of node
     * @param type
     *            leaf or collection
     * @param topicType
     *            schema of data on the node
     * @return true if node was created
     */
    public boolean addNode(String folder, String topic, NODE_ITEM_TYPE type, String topicType);

    /**
     * Remove a node from a collection node
     * 
     * @param folder
     *            collection node
     * @param topic
     *            name of node
     * @return true if node was removed
     */
    public boolean removeNode(String folder, String topic);

    /**
     * Remove a node
     * 
     * @param node
     * @return true if the node was removed
     */
    public boolean removeNode(String node);

    /**
     * Add a new collection node
     * 
     * @param folder
     * @param name
     * @return full name of the created node
     */
    public String addFolder(String folder, String name);

    /**
     * Add a new collectionf for the root of a interest group
     * 
     * @param interestGroupRoot
     * @return true if node was created
     */
    public boolean addCollection(String interestGroupRoot);

}