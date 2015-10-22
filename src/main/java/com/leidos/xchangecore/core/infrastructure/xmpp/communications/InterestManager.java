package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.springframework.integration.MessageChannel;

import com.leidos.xchangecore.core.infrastructure.xmpp.communications.NodeManagerImpl.NODE_ITEM_TYPE;

public interface InterestManager {

    public boolean addCollection(String pubsubService, String interestGroupRoot);

    /**
     * Add a folder to a folder.
     *
     * @param folder
     *            folder to create the new folder in
     * @param name
     *            name of new folder
     * @return fully qualified name of the folder that can be used for another call to addFolder
     */
    public String addFolder(String pubsubService, String folder, String name);

    public void addIQListener(PacketListener listener, PacketFilter filter);

    public void addMessageListener(PacketListener listener, PacketFilter filter);

    public boolean addNode(String pubsubService,
                           String folder,
                           String topic,
                           NODE_ITEM_TYPE type,
                           String topicType);

    public void addNodeManager(String pubsubService);

    @PreDestroy
    public void cleanup();

    public ArrayList<String> getAllNodeItems(String pubsubService, String node)
        throws IllegalArgumentException;

    public XmppConnection getCoreConnection();

    public DiscoverItems getFolderContents(String pubsubService, String interestGroupNode);

    public NodeManager getNodeManager(String pubsubService);

    // RDW remove
    // public List<String> subscribeToOwnersNode(String node,
    // Properties ownerConnectionProps) throws Exception;

    public MessageChannel getOwningCoreWorkProductNotificationChannel();

    /**
     * @return JID representing the InterestManager at this core
     */
    public String getOwnJid();

    /**
     * Method to initialize this object. After calling this isInitialized will return true and this
     * object can be used.
     *
     * @param con
     *            the CoreConnection to the XMPP server
     */
    @PostConstruct
    public void initialize();

    /**
     * @return true if this object has been initialized and can be used.
     */
    public boolean isInitialized();

    public boolean publishToNode(String pubsubService, String nodeName, String itemText);

    public void refreshSubscriptions(String pubsubService);

    public boolean removeItem(String pubsubService, String nodeName, String itemUUID);

    public boolean removeNode(String pubsubService, String node);

    public boolean removeNode(String pubsubService, String folder, String topic);

    public String retrieveNodeItem(String pubsubService, String node, String wpID)
        throws IllegalStateException, IllegalArgumentException;

    public void sendCleanupJoinedInterestGroupMessage(String remoteJID, String interestGroupID);

    public void sendDeleteJoinedInterestGroupMessage(String coreJID, String interestGroupID);

    public void sendDeleteJoinedProductMessage(String coreJID, String productID);

    public void sendJoinedPublishProductRequestMessage(String interestGroupId,
                                                       String owningCore,
                                                       String productId,
                                                       String productType,
                                                       String act,
                                                       String userID,
                                                       String product);

    public void sendJoinMessage(String coreJID,
                                InterestGroup interestGroup,
                                String interestGroupInfo,
                                List<String> workProductTypesToShare);

    public void sendProductPublicationStatusMessage(String requestingCore,
                                                    String userID,
                                                    String status);

    public void sendResignMessage(String coreJID);

    public void sendResignMessage(String coreJID, String interestGroupID);

    public void sendResignMessage(String coreJID, String interestGroupID, String interestGroupName);

    void sendResignRequestMessage(String coreJID, String interestGroupID, String interestGroupOwner);

    public void sendUpdateJoinMessage(String coreJID, String interestGroupID, String productType);

    public void setCoreConnection(CoreConnection coreConnection);

    public void setNodeManager(NodeManager nodeManager);

    public void setOwningCoreWorkProductNotificationChannel(MessageChannel owningCoreWorkProductNotificationChannel);

    public List<String> subscribeToNode(String pubsubService, String node) throws XMPPException;

    /**
     * Unsubscribe from all nodes that the JID for the connection is subscribed to.
     *
     */
    public void unsubscribeAll(String pubsubService);

    /**
     * Unsubscribe from all nodes that the JID for the connection is subscribed to for the given
     * interest group UUID.
     *
     * @param uuid
     *            UUID of the interest group to unsubscribe from
     */
    public void unsubscribeAllForInterestGroup(String pubsubService, String uuid);

    public void updateSubscriptionMap(String pubsubService);
}