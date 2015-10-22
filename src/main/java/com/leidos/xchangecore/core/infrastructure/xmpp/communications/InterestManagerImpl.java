package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;

import com.leidos.xchangecore.core.infrastructure.messages.PublishProductMessage;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.NodeManagerImpl.NODE_ITEM_TYPE;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt.InterestGrptManagementIQFactory;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.PubSubEventExtension;

/**
 * This class provides an interface to the XMPP storage of interest group related information <br />
 * <b>Todo:</b>
 * <ul>
 * <li>documenation - continue documenting</li>
 * </ul>
 *
 * <pre>
 * InterestManagement interestManager = new InterestMangement();
 *
 * // establish connection to server and login
 * interestManager.connect(&quot;username&quot;, &quot;password&quot;);
 *
 * // get the list of current interest groups
 *
 * </pre>
 *
 */

public class InterestManagerImpl
    implements InterestManager {

    // Class to handle the pubsub event messages that are notfications of changes in
    // work products.
    protected class ListenerAdapter
        implements PacketListener {

        public ListenerAdapter() {

        }

        @Override
        public void processPacket(Packet packet) {

            final Pattern retractPattern = Pattern.compile("retract\\s+id=[\"']([\\w-]+)[\"']");

            // TODO: this may not be the right place for this logic
            logger.debug("InterestManager:ListenerAdapter:processPacket: " + packet.toXML());
            final PacketExtension ext = packet.getExtension("http://jabber.org/protocol/pubsub#event");
            if (ext != null && ext instanceof PubSubEventExtension) {
                // logger.debug("    GOT a PubSubEventExtension");
                final PubSubEventExtension pubsub = (PubSubEventExtension) ext;

                // logger.debug("   pubsub: "+pubsub.toXML());
                // Handle item updates
                Iterator<com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.Item> it = pubsub.getItems();
                while (it.hasNext()) {
                    final String item = it.next().toXML();
                    // logger.debug("    ITEM: " + item);

                    // send out the node data to the Communications Service
                    final PublishProductMessage message = new PublishProductMessage(item,
                                                                                    coreConnection.getServer());
                    final org.springframework.integration.Message<PublishProductMessage> notification = new GenericMessage<PublishProductMessage>(message);
                    if (owningCoreWorkProductNotificationChannel != null) {
                        logger.info("********** Sending product publication to CommunicationsService");
                        owningCoreWorkProductNotificationChannel.send(notification);
                    } else
                        logger.error("owningCoreWorkProductNotificationChannel is null");
                }

                // Handle item retracts
                it = pubsub.getRetracts();
                while (it.hasNext()) {
                    final String xml = it.next().toXML();
                    final Matcher m = retractPattern.matcher(xml);
                    if (!m.find())
                        logger.error("Retract for unknown item: " + xml);
                }

                // Handle node deletes
                it = pubsub.getDeletes();
                // TODO: should we delete nodes on resign or allow joined cores to keep data?
                while (it.hasNext())
                    // TODO: Do not always get the right <delete node> messages ... need more work
                    // here
                    // In the mean time, we will use implicit delete IQ to communicate node
                    // deletions
                    logger.debug("===> NODE DELETE: " + it.next().toXML());
            }
        }
    }

    private final Logger logger = Logger.getLogger(this.getClass());

    private CoreConnection coreConnection;

    // key = pubsub service name for a particular XMPP server
    private final HashMap<String, NodeManager> nodeManagers;

    private final ListenerAdapter listenerAdapter;

    private MessageChannel owningCoreWorkProductNotificationChannel;

    /**
     * Default constructor, must call initialize before using this object
     */
    public InterestManagerImpl() {

        listenerAdapter = new ListenerAdapter();
        nodeManagers = new HashMap<String, NodeManager>();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#addCollection
     * (java.lang.String)
     */
    @Override
    public boolean addCollection(String pubsubService, String interestGroupRoot) {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).addCollection(interestGroupRoot);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#addFolder(
     * java.lang.String, java.lang.String)
     */
    @Override
    public String addFolder(String pubsubService, String folder, String name) {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).addFolder(folder, name);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#addIQListener
     * (org.jivesoftware.smack.PacketListener, org.jivesoftware.smack.filter.PacketFilter)
     */
    @Override
    public void addIQListener(PacketListener listener, PacketFilter filter) {

        final PacketTypeFilter iqFilter = new PacketTypeFilter(IQ.class);
        final AndFilter andFilter = new AndFilter(iqFilter, filter);

        coreConnection.addPacketListener(listener, andFilter);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#addMessageListener
     * (org.jivesoftware.smack.PacketListener, org.jivesoftware.smack.filter.PacketFilter)
     */
    @Override
    public void addMessageListener(PacketListener listener, PacketFilter filter) {

        final PacketTypeFilter msgFilter = new PacketTypeFilter(Message.class);
        final AndFilter andFilter = new AndFilter(msgFilter, filter);

        coreConnection.addPacketListener(listener, andFilter);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#addNode(java
     * .lang.String, java.lang.String,
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.NodeManager.NODE_ITEM_TYPE,
     * java.lang.String)
     */
    @Override
    public boolean addNode(String pubsubService,
                           String folder,
                           String topic,
                           NODE_ITEM_TYPE type,
                           String topicType) {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).addNode(folder, topic, type, topicType);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    @Override
    public void addNodeManager(String pubsubService) {

        if (getNodeManager(pubsubService) == null) {
            // RDW should factory this for testing
            final NodeManager nodeManager = new NodeManagerImpl(coreConnection);
            nodeManager.setPubsubService(pubsubService);
            nodeManagers.put(pubsubService, nodeManager);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#cleanup()
     */
    @Override
    @PreDestroy
    public void cleanup() {

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#getAllNodeItems
     * (java.lang.String)
     */
    @Override
    public ArrayList<String> getAllNodeItems(String pubsubService, String node)
        throws IllegalArgumentException {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).getAllNodeItems(node);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return new ArrayList<String>();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#getCoreConnection
     * ()
     */
    @Override
    public CoreConnection getCoreConnection() {

        return coreConnection;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#getFolderContents
     * (java.lang.String)
     */
    @Override
    public DiscoverItems getFolderContents(String pubsubService, String interestGroupNode) {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).getChildrenNodes(interestGroupNode);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#getNodeManager
     * (java.lang.String)
     */
    @Override
    public NodeManager getNodeManager(String pubsubService) {

        NodeManager nodeManager = null;
        if (pubsubService != null)
            if (nodeManagers.containsKey(pubsubService))
                nodeManager = nodeManagers.get(pubsubService);
        return nodeManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#
     * getOwningCoreWorkProductNotificationChannel()
     */
    @Override
    public MessageChannel getOwningCoreWorkProductNotificationChannel() {

        return owningCoreWorkProductNotificationChannel;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#getOwnJid()
     */
    @Override
    public String getOwnJid() {

        return coreConnection.getJID();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#initialize()
     */
    @Override
    @PostConstruct
    public void initialize() {

        logger.debug("Initialize called - coreConnection is initialized? " + coreConnection);
        if (!coreConnection.isConnected())
            coreConnection.initialize();

        // create a node manager for the main XMPP connection
        if (nodeManagers.isEmpty())
            addNodeManager(coreConnection.getPubSubSvc());

        assert coreConnection != null;

        logger.debug("initialized done");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#isInitialized
     * ()
     */
    @Override
    public boolean isInitialized() {

        return coreConnection != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#publishToNode
     * (java.lang.String, java.lang.String)
     */
    @Override
    public boolean publishToNode(String pubsubService, String nodeName, String itemText) {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).publishToNode(nodeName, itemText);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#
     * refreshSubscriptions()
     */
    @Override
    public void refreshSubscriptions(String pubsubService) {

        if (nodeManagers.containsKey(pubsubService))
            nodeManagers.get(pubsubService).updateSubscriptionMap();
        else
            logger.error("No node manager for pubsub service: " + pubsubService);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#removeItem
     * (java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeItem(String pubsubService, String nodeName, String itemUUID) {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).removeItem(nodeName, itemUUID);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    // /* (non-Javadoc)
    // * @see
    // com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#unsubscribe(java.lang.String)
    // */
    // private void unsubscribe(String node) {
    // nodeSubscriptionManager.unsubscribe(node);
    // }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#removeNode
     * (java.lang.String)
     */
    @Override
    public boolean removeNode(String pubsubService, String node) {

        logger.debug("removeNode: node: " + node);
        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).removeNode(node);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#removeNode
     * (java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeNode(String pubsubService, String folder, String topic) {

        logger.debug("removeNode: folder: " + folder + ", topic: " + topic);
        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).removeNode(folder, topic);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#retrieveNodeItem
     * (java.lang.String, java.lang.String)
     */
    @Override
    public String retrieveNodeItem(String pubsubService, String node, String wpID)
        throws IllegalStateException, IllegalArgumentException {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).retrieveNodeItem(node, wpID);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

    @Override
    public void sendCleanupJoinedInterestGroupMessage(String remoteJID, String interestGroupID) {

        logger.info("sendCleanupJoinedInterestGroupMessage: remoteJID: " + remoteJID + ", IGID: " +
                    interestGroupID);
        final String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(remoteJID);
        final IQ msg = InterestGrptManagementIQFactory.createCleanupJoinedInterestGroupMessage(
            coreJIDPlusResource, interestGroupID);

        // logger.debug("sendDeleteJoinedInterestGroupMessage: [" + msg.toXML() + "]");

        // Fire off the delete interest group message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("sendDeleteJoinedInterestGroupMessage: " + e.getMessage());
        }
    }

    @Override
    public void sendDeleteJoinedInterestGroupMessage(String coreJID, String interestGroupID) {

        logger.info("sendDeleteJoinedInterestGroupMessage: remoteJID: " + coreJID + ", IGID: " +
                    interestGroupID);
        final String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        final IQ msg = InterestGrptManagementIQFactory.createDeleteJoinedInterestGroupMessage(
            coreJIDPlusResource, interestGroupID);

        // logger.debug("sendDeleteJoinedInterestGroupMessage: [" + msg.toXML() + "]");

        // Fire off the delete interest group message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("sendDeleteJoinedInterestGroupMessage: " + e.getMessage());
        }

    }

    @Override
    public void sendDeleteJoinedProductMessage(String coreJID, String productID) {

        logger.info("sendDeleteJoinedProductMessage");
        final String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        final IQ msg = InterestGrptManagementIQFactory.createDeleteJoinedProductMessage(
            coreJIDPlusResource, productID);

        logger.debug(msg.toXML());

        // Fire off the delete interest group message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending delete joined product message: " + e.getMessage());
            logger.debug("sendDeleteJoinedProductMessage: " + msg.toXML());
        }

    }

    @Override
    public void sendJoinedPublishProductRequestMessage(String interestGroupId,
                                                       String owningCore,
                                                       String productId,
                                                       String productType,
                                                       String act,
                                                       String userID,
                                                       String product) {

        logger.info("sendJoinedPublishProductRequestMessage: interestGroupID: " + interestGroupId +
                    " owningCore:" + owningCore);

        final String owningCoreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(owningCore);

        // TODO: Need the other stuff, e.g. name, description, lat/long ???
        final StringBuffer params = new StringBuffer();
        params.append(" interestGroupId='" + interestGroupId + "'");
        params.append(" productId='" + productId + "'");
        params.append(" productType='" + productType + "'");
        params.append(" act='" + act + "'");
        params.append(" userID='" + userID + "'");
        params.append(" owningCore='" + owningCore + "'");
        params.append(" requestingCore='" + coreConnection.getCoreNameFromJID(getOwnJid()) + "'>");

        final HashMap<String, String> config = new HashMap<String, String>();

        final StringBuffer productEntry = new StringBuffer();
        productEntry.append("<ProductPayload>");
        productEntry.append(product);
        productEntry.append("</ProductPayload>");

        final IQ msg = InterestGrptManagementIQFactory.createJoinedPublishProductRequestMessage(
            owningCoreJIDPlusResource, params.toString(), config, productEntry.toString());

        logger.debug(msg.toXML());

        // Fire off the join request
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending join published product request message: " + e.getMessage());
            logger.debug("sendJoinedPublishProductRequestMessage: " + msg.toXML());
        }
    }

    @Override
    public void sendJoinMessage(String coreJID,
                                InterestGroup interestGroup,
                                String interestGroupInfo,
                                List<String> workProductTypesToShare) {

        logger.info("sendJoinMessage to " + coreJID + " with interestGroupID=" +
                    interestGroup.interestGroupID);
        final HashMap<String, String> config = new HashMap<String, String>();

        final StringBuffer sb = new StringBuffer();

        sb.append("<workProductTypesToShare>");
        for (final String workProductType : workProductTypesToShare) {
            sb.append("<item>");
            sb.append(workProductType);
            sb.append("</item>");
        }
        sb.append("</workProductTypesToShare>");

        sb.append("<info>");
        sb.append(interestGroupInfo);
        sb.append("</info>");

        final StringBuffer interestGroupInfoBuffer = new StringBuffer();
        interestGroupInfoBuffer.append(" uuid='" + interestGroup.interestGroupID + "'");
        interestGroupInfoBuffer.append(" interestGroupType='" + interestGroup.interestGroupType +
                                       "'");
        interestGroupInfoBuffer.append(" owner='" + getOwnJid() + "'");

        final String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);

        final IQ msg = InterestGrptManagementIQFactory.createJoinMessage(coreJIDPlusResource,
            interestGroupInfoBuffer.toString(), config, sb.toString());

        logger.debug(msg.toXML());

        // Fire off the join request
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending join message: " + e.getMessage());
            logger.debug("sendJoinMessage: " + msg.toXML());
        }
    }

    @Override
    public void sendProductPublicationStatusMessage(String requestingCore,
                                                    String userID,
                                                    String status) {

        logger.info("sendProductPublicationStatusMessage: requestingCore=" + requestingCore +
                    " userID:" + userID + " status=[" + status + "]");

        final String requestingCoreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(requestingCore);

        final StringBuffer params = new StringBuffer();
        params.append(" userID='" + userID + "'");
        params.append(" owningCore='" + coreConnection.getCoreNameFromJID(getOwnJid()) + "'>");

        final StringBuffer statustEntry = new StringBuffer();
        statustEntry.append("<ProductPublicationStatus>");
        statustEntry.append(status);
        statustEntry.append("</ProductPublicationStatus>");

        final IQ msg = InterestGrptManagementIQFactory.createProductPublicationStatusMessage(
            requestingCoreJIDPlusResource, params.toString(), statustEntry.toString());

        // logger.debug(msg.toXML());

        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending product publication status message: " + e.getMessage());
            logger.debug("sendProductPublicationStatusMessage: " + msg.toXML());
        }
    }

    @Override
    public void sendResignMessage(String remoteJID) {

        logger.debug("sendResignMessage: remote core: " + remoteJID);
        sendResignMessage(remoteJID, "", "");
    }

    @Override
    public void sendResignMessage(String coreJID, String interestGroupID) {

        sendResignMessage(coreJID, interestGroupID, "");
    }

    @Override
    public void sendResignMessage(String coreJID, String interestGroupID, String interestGroupName) {

        final String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        logger.debug("sendResignMessage: remoteJID: " + coreJIDPlusResource + ", IGID: " +
                     interestGroupID + ", IG Name: " + interestGroupName);
        final IQ msg = InterestGrptManagementIQFactory.createResignMessage(coreJIDPlusResource,
            interestGroupID, interestGroupName, getOwnJid());

        // Fire off the resign message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending resign message: " + e.getMessage());
            logger.debug("sendResignMessage: " + msg.toXML());
        }

    }

    @Override
    public void sendResignRequestMessage(String coreJID,
                                         String interestGroupID,
                                         String interestGroupOwner) {

        logger.debug("sendResignRequestMessage");
        final String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        final IQ msg = InterestGrptManagementIQFactory.createResigRequestMessage(
            coreJIDPlusResource, interestGroupID, interestGroupOwner);

        // logger.debug(msg.toXML());

        // Fire off the resign request
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending resign request message: " + e.getMessage());
            logger.debug("sendResignRequestMessage: " + msg.toXML());
        }

    }

    @Override
    public void sendUpdateJoinMessage(String coreJID, String interestGroupID, String productType) {

        logger.info("sendUpdateJoinMessage  interestGroupID=" + interestGroupID + " wpType=" +
                    productType);
        final String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        final IQ msg = InterestGrptManagementIQFactory.createUpdateJoinMessage(coreJIDPlusResource,
            interestGroupID, productType);

        logger.debug(msg.toXML());

        // Fire off the update join message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending update join message: " + e.getMessage());
            logger.debug("sendUpdateJoinMessage: " + msg.toXML());
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#setCoreConnection
     * (com.leidos.xchangecore.core.infrastructure.xmpp.communications.CoreConnection)
     */
    @Override
    public void setCoreConnection(CoreConnection coreConnection) {

        this.coreConnection = coreConnection;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#setNodeManager
     * (com.leidos.xchangecore.core.infrastructure.xmpp.communications.NodeManager)
     */
    @Override
    public void setNodeManager(NodeManager nodeManager) {

        nodeManagers.put(nodeManager.getPubsubService(), nodeManager);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#
     * setOwningCoreWorkProductNotificationChannel
     * (org.springframework.integration.core.MessageChannel)
     */
    @Override
    public void setOwningCoreWorkProductNotificationChannel(MessageChannel owningCoreWorkProductNotificationChannel) {

        this.owningCoreWorkProductNotificationChannel = owningCoreWorkProductNotificationChannel;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#subscribeToNode
     * (java.lang.String)
     */
    @Override
    public List<String> subscribeToNode(String pubsubService, String node) throws XMPPException {

        if (nodeManagers.containsKey(pubsubService))
            return nodeManagers.get(pubsubService).subscribeToNode(node, listenerAdapter);
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

    @Override
    public String toString() {

        final StringBuilder result = new StringBuilder();
        final String newLine = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " Object {");
        result.append(newLine);

        result.append(" Subscription Managers: ");
        for (final String key : nodeManagers.keySet()) {
            result.append(nodeManagers.get(key));
            result.append(newLine);
        }

        result.append("}");

        return result.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#unsubscribeAll
     * ()
     */
    @Override
    public void unsubscribeAll(String pubsubService) {

        if (nodeManagers.containsKey(pubsubService))
            nodeManagers.get(pubsubService).unsubscribeAll();
        else
            logger.error("No node manager for pubsub service: " + pubsubService);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#unsubscribeAll
     * (java.lang.String)
     */
    @Override
    public void unsubscribeAllForInterestGroup(String pubsubService, String uuid) {

        if (nodeManagers.containsKey(pubsubService))
            nodeManagers.get(pubsubService).unsubscribeAll(uuid);
        else
            logger.error("No node manager for pubsub service: " + pubsubService);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestManager#
     * updateSubscriptionMap()
     */
    @Override
    public void updateSubscriptionMap(String pubsubService) {

        if (nodeManagers.containsKey(pubsubService))
            nodeManagers.get(pubsubService).updateSubscriptionMap();
        else
            logger.error("No node manager for pubsub service: " + pubsubService);
    }

}
