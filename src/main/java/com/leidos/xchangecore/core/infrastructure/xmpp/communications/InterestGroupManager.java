package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;

import com.leidos.xchangecore.core.infrastructure.messages.DeleteInterestGroupForRemoteCoreMessage;
import com.leidos.xchangecore.core.infrastructure.messages.DeleteJoinedInterestGroupMessage;
import com.leidos.xchangecore.core.infrastructure.messages.DisseminationManagerMessage;
import com.leidos.xchangecore.core.infrastructure.messages.JoinedInterestGroupNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.messages.ShareInterestGroupMessage;
import com.leidos.xchangecore.core.infrastructure.util.LdapUtil;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.NodeManagerImpl.NODE_ITEM_TYPE;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt.InterestGrpManagementEventFactory;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt.InterestGrptManagementIQFactory;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory;

public class InterestGroupManager {

    public enum CORE_STATUS {
        OWNED, JOIN_IN_PROGRESS, JOINED, RESIGN_IN_PROGRESS, RESIGNED, ERROR
    }

    public static final String productsNodeSuffix = "_WorkProducts";

    private final Logger logger = Logger.getLogger(this.getClass());

    private final Object processSuspendedUpdatesLock = new Object();;

    // key is interestGroupID
    private HashMap<String, InterestGroup> ownedInterestGroups; // mostly GuardedBy("this")

    // key is interestGroupID.RemoteCore
    private HashMap<String, InterestGroup> joinedInterestGroups; // mostly GuardedBy("this")

    // key is interestGroupID, value is list of joining core names
    private HashMap<String, List<String>> joiningCores; // GuardedBy("this")

    private final ArrayList<InterestGroup> failedJoins = new ArrayList<InterestGroup>();

    private CoreConnection coreConnection;
    private InterestManager interestManager;
    private LdapUtil ldapUtil;

    private MessageChannel joinedPublishProductNotificationChannel;
    private MessageChannel joinedInterestGroupNotificationChannel;
    private MessageChannel productPublicationStatusNotificationChannel;
    private MessageChannel deleteJoinedInterestGroupNotificationChannel;
    private MessageChannel deleteJoinedProductNotificationChannel;
    private MessageChannel deleteInterestGroupSharedFromRemoteCoreChannel;
    /**
     * The dissemination manager message channel.
     */
    private MessageChannel disseminationManagerChannel;

    private synchronized void addJoiningCoreToInterestGroup(InterestGroup interestGroup,
                                                            String joinedCore) {

        logger.debug("addJoiningCoreToInterestGroup: add JID: " + joinedCore + " to IGID: " +
            interestGroup.interestGroupID);
        joiningCores.get(interestGroup.interestGroupID).add(joinedCore);
    }

    public synchronized void addToFailedJoins(InterestGroup interestGroup) {

        if (!failedJoins.contains(interestGroup)) {
            failedJoins.add(interestGroup);
        }
    }

    private synchronized void addToJoiningCoresList(InterestGroup interestGroup) {

        joiningCores.put(interestGroup.interestGroupID, new ArrayList<String>());
    }

    public void cleanupInterestGroupSharedFromRemoteCore(String remoteJID, String interestGroupID) {

        final DeleteInterestGroupForRemoteCoreMessage message = new DeleteInterestGroupForRemoteCoreMessage(remoteJID,
                                                                                                            interestGroupID);
        final Message<DeleteInterestGroupForRemoteCoreMessage> deleteMessage = new GenericMessage<DeleteInterestGroupForRemoteCoreMessage>(message);
        getDeleteInterestGroupSharedFromRemoteCoreChannel().send(deleteMessage);
    }

    public synchronized void clearJoinInProgress(String interestGroupID) {

        if (ownedInterestGroups.containsKey(interestGroupID)) {
            if (ownedInterestGroups.get(interestGroupID).state == CORE_STATUS.JOIN_IN_PROGRESS) {
                logger.info("Clearing JOIN_IN_PROGRESS from " + interestGroupID);
                ownedInterestGroups.get(interestGroupID).state = CORE_STATUS.OWNED;
            }
        }
    }

    public String createInterestGroup(InterestGroup interestGroup) {

        if (interestGroup != null) {
            logger.info("createInterestGroup: interestGroupID=" + interestGroup.interestGroupID +
                        " interestGroupType=" + interestGroup.interestGroupType);

            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.state = CORE_STATUS.OWNED;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            // add to interest group status map
            // log.debug("createInterestGroup: add interest group to map");
            ownedInterestGroups.put(interestGroup.interestGroupID, interestGroup);

            // create array to store future joining cores
            addToJoiningCoresList(interestGroup);

            // Add the products node
            // log.debug("createInterestGroup: addNode " + interestGroup.interestGroupNode + " to ["
            // + coreConnection.getInterestGroupRoot() + "]");
            // interestManager.addNode(coreConnection.getInterestGroupRoot(),
            // interestGroup.interestGroupNode, NODE_ITEM_TYPE.ITEM_LIST, "");
            // log.debug("createInterestGroup: addFolder " + interestGroup.interestGroupNode +
            // " to ["
            // + coreConnection.getInterestGroupRoot() + "]");

            // add a node manager for the interest groups pubsub service
            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // add a root folder
            // RDW - need to figure out what to do when the return from addFolder is null
            // i.e. the creation of the node failed.
            interestManager.addFolder(interestGroup.interestGroupPubsubService,
                                      coreConnection.getInterestGroupRoot(),
                                      interestGroup.interestGroupNode);

            // update the subscription map
            interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

            return interestGroup.interestGroupID;
        } else {
            return null;
        }
    }

    private boolean createInterestGroupRoot() {

        // See if there is a /interest group node
        logger.info("createInterestGroupRoot: " + coreConnection.getInterestGroupRoot());
        boolean hasInterestGroupRoot = false;
        DiscoverItems discoItems;
        logger.info("===> perform discovery for pubsubsvc: " + coreConnection.getPubSubSvc());
        discoItems = coreConnection.discoverNodeItems("");

        if (discoItems != null) {
            // Get the discovered items of the queried XMPP entity
            final Iterator<DiscoverItems.Item> it = discoItems.getItems();
            // Display the items of the remote XMPP entity
            while (it.hasNext()) {

                final DiscoverItems.Item item = it.next();

                if (item.getName().equals(coreConnection.getInterestGroupRoot())) {
                    hasInterestGroupRoot = true;
                }
            }
        }

        if (!hasInterestGroupRoot) {
            logger.info("Add interestGroup root collection");
            hasInterestGroupRoot = interestManager.addCollection(coreConnection.getPubSubSvc(),
                                                                 coreConnection.getInterestGroupRoot());
            if (!hasInterestGroupRoot) {
                logger.error("Root collection not created");
            }
        }
        return hasInterestGroupRoot;
    }

    /*
     * public void coreStatusUpdateHandler(CoreStatusUpdateMessage message) {
     *
     * String coreName = message.getCoreName(); String coreStatus = message.getCoreStatus();
     *
     * if (coreName.endsWith("/CoreConnection")) { coreName = coreName.substring(0,
     * coreName.indexOf("/CoreConnection")); }
     *
     * logger.debug("coreStatusUpdateHandler: coreName: " + coreName + ", status: " + coreStatus);
     * if (coreStatus.equals("available")) {
     * logger.debug("coreStatusUpdateHandler: recover/restore the saved messages now ...");
     * List<String> messageSet = getQueuedMessageDAO().getMessagesByCorename(coreName); if
     * (messageSet != null) { logger.debug("coreStatusUpdateHandler: found " + messageSet.size() +
     * " interest groups to recover"); for (String m : messageSet) { //
     * logger.debug("coreStatusUpdateHandler: message:\n[" + m + "\n]"); Object o =
     * SerializerUtil.deserialize(m); if (o instanceof ShareInterestGroupMessage) {
     * shareInterestGroup((ShareInterestGroupMessage) o); } } } } }
     */

    public void deleteInerestGroupAtJoinedCore(String remoteJID) {

        logger.debug("deleteInerestGroupAtJoinedCore: retry to remove incidents which shared with " +
            remoteJID);
        final ArrayList<String> interestGroupIDList = new ArrayList<String>();
        for (final String interestGroupID : joiningCores.keySet()) {
            if (joiningCores.get(interestGroupID).contains(remoteJID)) {
                logger.debug("deleteInerestGroupAtJoinedCore: interestGroupID: " + interestGroupID +
                             " is shared with remote core: " + remoteJID);
                interestGroupIDList.add(interestGroupID);
            }
        }
        logger.debug("deleteInerestGroupAtJoinedCore: total " + interestGroupIDList.size() +
            " IGs are shared");
        for (final String interestGroupID : interestGroupIDList) {
            logger.debug("deleteInerestGroupAtJoinedCore: delete interestGroupID: " +
                interestGroupID + " @ " + remoteJID);
            // TODO we will let Core2CoreMessage to handle this at remote core
            // interestManager.sendCleanupJoinedInterestGroupMessage(remoteJID, interestGroupID);
            removeJoiningCoreFromInterestGroup(interestGroupID, remoteJID);
        }
    }

    public void deleteInterestGroup(String interestGroupID) {

        logger.debug("deleteInterestGroup: interestGroupID=" + interestGroupID);

        final InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
        if (interestGroup != null) {

            final Set<String> joinedCoreJIDs = interestGroup.joinedCoreJIDMap.keySet();
            for (final String joinedCoreJID : joinedCoreJIDs) {
                interestManager.sendDeleteJoinedInterestGroupMessage(joinedCoreJID, interestGroupID);
            }

            // delete all the work product type nodes
            for (final String wpType : interestGroup.workProductTypes) {
                final String wpTypeNode = wpType + "_" + interestGroupID;
                interestManager.removeNode(interestGroup.interestGroupPubsubService, wpTypeNode);
            }

            // Unsubscribe to all nodes for this interest group
            interestManager.unsubscribeAllForInterestGroup(interestGroup.interestGroupPubsubService,
                                                           interestGroupID);

            // delete the interest group node
            interestManager.removeNode(interestGroup.interestGroupPubsubService,
                                       interestGroup.interestGroupNode);

            // add to interest group status map
            // log.debug("createInterestGroup: add interest group to map");
            synchronized (this) {
                ownedInterestGroups.remove(interestGroup);
            }

            // update the subscription map
            interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

        }
    }

    public void deleteJoinedInterestGroup(String interestGroupID) {

        if (joinedInterestGroups.containsKey(interestGroupID)) {
            synchronized (this) {
                joinedInterestGroups.remove(interestGroupID);
            }
        }

        final DeleteJoinedInterestGroupMessage message = new DeleteJoinedInterestGroupMessage();
        message.setInterestGroupID(interestGroupID);
        final Message<DeleteJoinedInterestGroupMessage> notification = new GenericMessage<DeleteJoinedInterestGroupMessage>(message);
        getDeleteJoinedInterestGroupNotificationChannel().send(notification);

    }

    public void deleteWorkProduct(String wpID, String wpType, String interestGroupID) {

        logger.info("deleteWorkProduct: interestGroupID=" + interestGroupID + " wpType=" + wpType +
                    " wpID=" + wpID);

        final InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
        if (interestGroup != null) {

            final Set<String> joinedCoreJIDs = interestGroup.joinedCoreJIDMap.keySet();
            for (final String joinedCoreJID : joinedCoreJIDs) {
                interestManager.sendDeleteJoinedProductMessage(joinedCoreJID, wpID);
            }

            if (interestGroup.workProductTypes.contains(wpType)) {
                final String wpTypeNode = wpType + "_" + interestGroupID;
                try {
                    if (interestManager.retrieveNodeItem(interestGroup.interestGroupPubsubService,
                                                         wpTypeNode,
                                                         wpID) != null) {
                        interestManager.removeItem(interestGroup.interestGroupPubsubService,
                                                   wpTypeNode,
                                                   wpID);
                    }
                } catch (final Exception e) {
                    logger.error("deleteWorkProduct: [" + wpID + "]: " + e.getMessage());
                }
            }
        }
    }

    public CoreConnection getCoreConnection() {

        return coreConnection;
    }

    public MessageChannel getDeleteInterestGroupSharedFromRemoteCoreChannel() {

        return deleteInterestGroupSharedFromRemoteCoreChannel;
    }

    public MessageChannel getDeleteJoinedInterestGroupNotificationChannel() {

        return deleteJoinedInterestGroupNotificationChannel;
    }

    public MessageChannel getDeleteJoinedProductNotificationChannel() {

        return deleteJoinedProductNotificationChannel;
    }

    public synchronized List<InterestGroup> getFailedJoins() {

        return Collections.unmodifiableList(failedJoins);
    }

    public Map<String, InterestGroup> getInterestGroupList() {

        return Collections.unmodifiableMap(ownedInterestGroups);
    }

    // support for ldap and dissemination Manager

    public InterestManager getInterestManager() {

        return interestManager;
    }

    public InterestGroup getJoinedInterestGroup(String interestGroupID, String coreName) {

        return ownedInterestGroups.get(interestGroupID + "." + coreName);
    }

    public MessageChannel getJoinedInterestGroupNotificationChannel() {

        return joinedInterestGroupNotificationChannel;
    }

    public HashMap<String, InterestGroup> getJoinedInterestGroups() {

        return joinedInterestGroups;
    }

    public MessageChannel getJoinedPublishProductNotificationChannel() {

        return joinedPublishProductNotificationChannel;
    }

    private synchronized List<String> getJoiningCoresList(String interestGroupID) {

        final List<String> coreList = joiningCores.get(interestGroupID);
        return coreList == null ? null : Collections.unmodifiableList(coreList);
    }

    public LdapUtil getLdapUtil() {

        return ldapUtil;
    }

    public InterestGroup getOwnedInterestGroup(String interestGroupID) {

        return ownedInterestGroups.get(interestGroupID);
    }

    public HashMap<String, InterestGroup> getOwnedInterestGroups() {

        return ownedInterestGroups;
    }

    public Object getProcessSuspendedUpdatesLock() {

        return processSuspendedUpdatesLock;
    }

    public MessageChannel getProductPublicationStatusNotificationChannel() {

        return productPublicationStatusNotificationChannel;
    }

    public String getWorkProduct(String interestGroupID, String wpID)
        throws IllegalArgumentException {

        final InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);

        logger.info("getWorkProduct for interestGroupID=" + interestGroupID + " wpID=" + wpID);

        String wp = null;
        if (interestGroup != null) {
            wp = interestManager.retrieveNodeItem(interestGroup.interestGroupPubsubService,
                                                  interestGroup.interestGroupNode,
                                                  wpID);
        } else {
            logger.error("Unable to find interestGroup:[" + interestGroupID + "] in map");
            throw new IllegalArgumentException("getWorkProduct: unable to find interestGroup " +
                interestGroupID + " in map", null);
        }
        return wp;
    }

    public void handleNewInterestGroup(String interestGroupID) {

        logger.debug("handleNewInterestGroup: received notification of joined interest group id=" +
            interestGroupID);
        final DisseminationManagerMessage dissMsg = new DisseminationManagerMessage();

        // set the interest group ID
        dissMsg.setInterestGroupID(interestGroupID);

        // Share to everyone
        ArrayList<String> groupJids = getLdapUtil().getGroupMembersForUsers();
        for (final String jid : groupJids) {
            if (!dissMsg.getJidsToAdd().contains(jid)) {
                dissMsg.addJID(jid);
            }
        }

        // Share to uicds-admin group as well
        groupJids = getLdapUtil().getGroupMembersForAdmins();
        for (final String jid : groupJids) {
            if (!dissMsg.getJidsToAdd().contains(jid)) {
                dissMsg.addJID(jid);
            }
        }

        final Message<DisseminationManagerMessage> message = new GenericMessage<DisseminationManagerMessage>(dissMsg);
        disseminationManagerChannel.send(message);
    }

    @PostConstruct
    public void initialize() {

        logger.debug("xmpp/InterestGroupManager:initialize()");
        assert coreConnection != null;
        assert interestManager != null;

        interestManager.addIQListener(new InterestGrpGMgmtIQListener(this),
                                      new IQNamespacePacketFilter(InterestGrptManagementIQFactory.namespace));

        interestManager.addMessageListener(new InterestGrpMgmtEventListener(this),
                                           new PacketExtensionFilter(InterestGrpManagementEventFactory.ELEMENT_NAME,
                                                                     InterestGrpManagementEventFactory.NAMESPACE));

        createInterestGroupRoot();

        // Create a map to manage the locally created interest groups
        ownedInterestGroups = new HashMap<String, InterestGroup>();

        // Create a map to manage joined interest groups
        joinedInterestGroups = new HashMap<String, InterestGroup>();

        // Map for cores in the process of getting joined <uuid, core name>
        joiningCores = new HashMap<String, List<String>>();
    }

    public boolean interestGroupExists(String interestGroupID) {

        final InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
        return interestGroup != null;
    }

    public synchronized boolean isCoreJoining(String coreName, String interestGroupID)
        throws IllegalArgumentException {

        boolean found = false;
        if ((interestGroupID != null) && joiningCores.containsKey(interestGroupID)) {
            if (getJoiningCoresList(interestGroupID).contains(coreName)) {
                found = true;
            }
        } else {
            throw new IllegalArgumentException("InterestGroupManager:isCoreJoining unknown interest group");
        }
        return found;
    }

    /**
     * Returns true if this core is joined to the interest group with the given uuid.
     *
     * @param uuid
     *            uuid unique id of an interest group
     * @return true if this core is joined to the interest group
     */
    // TODO: is the core name in the joined key really needed???
    public synchronized boolean isInterestGroupJoined(String joinedkey) {

        return joinedInterestGroups.containsKey(joinedkey) &&
            (joinedInterestGroups.get(joinedkey) != null);
    }

    /**
     * Returns true if this core owns the interest group with the given uuid.
     *
     * @param uuid
     *            unique id of an interest group
     * @return true if interest group is owned by this core
     */
    public synchronized boolean isInterestGroupOwned(String uuid) {

        return ownedInterestGroups.containsKey(uuid) && (ownedInterestGroups.get(uuid) != null);
    }

    /**
     * Join an interest group. This core cannot own the interest group.
     *
     * @param interestGroupUUID
     *            - UUID of the interest group
     * @return a new instance of the InterestGroup class representing the interest group
     */

    // TODO: add interestGroupWPID
    // public boolean joinInterestGroup(String interestGroupID, String interestGroupName, String
    // interestGroupWPID,
    // String owner, Properties ownerConnectionProperties, String[] workProducts) {
    public boolean joinInterestGroup(InterestGroup interestGroup,
                                     String xmlPropsStr,
                                     String interestGroupInfo) throws XMPPException {

        logger.info("joinInterestGroup " + interestGroup.interestGroupID);

        // set up the dissemination manager message
        handleNewInterestGroup(interestGroup.interestGroupID);

        boolean joined = false;
        // Don't join if we own it or are already joined
        // TODO: Is the core JID in the joinedKey really needed ????
        // String joinedKey = interestGroup.interestGroupID + "." + coreConnection.getJID();
        final String joinedKey = interestGroup.interestGroupID;
        if (!isInterestGroupOwned(interestGroup.interestGroupID) &&
            !isInterestGroupJoined(joinedKey)) {

            // Create a local representation of the InterestGroup
            // and have it create the nodes and populate the
            // initial atom entry

            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;

            // RDW remove ownerProps from InterestGroup (also interestGroupAtomEntryText?)
            interestGroup.state = CORE_STATUS.JOINED;
            interestGroup.interestGroupInfo = "";
            interestGroup.suspendUpdateProcessing = false;

            // Add the joined interest group
            joinedInterestGroups.put(joinedKey, interestGroup);

            // Add a node manager for this pubsub service
            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // subscribe to the child nodes (specified workProductType's) at the owning core
            for (final String wpType : interestGroup.workProductTypes) {
                final String wpTypeNode = wpType + "_" + interestGroup.interestGroupID;
                logger.debug("joinInterestGroup - subscribing to node:" + wpTypeNode);
                try {
                    interestManager.subscribeToNode(interestGroup.interestGroupPubsubService,
                                                    wpTypeNode);
                } catch (final XMPPException e) {
                    logger.error("joinInterestGroup: Error subscribing to owner's node " +
                        wpTypeNode);
                    joinedInterestGroups.remove(joinedKey);
                    interestGroup.interestGroupInfo = interestGroupInfo;
                    addToFailedJoins(interestGroup);
                    throw e;
                } catch (final Exception e) {
                    logger.error("joinInterestGroup: Error subscribing to owner's node " +
                        wpTypeNode);
                    joinedInterestGroups.remove(joinedKey);
                    interestGroup.interestGroupInfo = interestGroupInfo;
                    addToFailedJoins(interestGroup);
                    return false;
                }

            }

            // TODO: should we send some confirmation back to owning core???

            // Tell COMMS about the new interestGroup here
            logger.debug("joinInterestGroup - notify Comms of joined interest group id=" +
                interestGroup.interestGroupID);
            final JoinedInterestGroupNotificationMessage joinedInterestGroupMessage = new JoinedInterestGroupNotificationMessage();
            joinedInterestGroupMessage.setInterestGroupID(interestGroup.interestGroupID);
            joinedInterestGroupMessage.setOwner(interestGroup.interestGroupOwner);
            joinedInterestGroupMessage.setOwnerProperties(xmlPropsStr);
            joinedInterestGroupMessage.setInterestGroupType(interestGroup.interestGroupType);
            joinedInterestGroupMessage.setInterestGroupInfo(interestGroupInfo);
            joinedInterestGroupMessage.setJoinedWPTypes(interestGroup.workProductTypes);
            final Message<JoinedInterestGroupNotificationMessage> notification = new GenericMessage<JoinedInterestGroupNotificationMessage>(joinedInterestGroupMessage);
            joinedInterestGroupNotificationChannel.send(notification);

            joined = true;

        } else if (isInterestGroupOwned(interestGroup.interestGroupID)) {
            throw new IllegalArgumentException("InterestGroupManager:joinInterestGroup trying to join " +
                interestGroup.interestGroupID +
                " but this core owns it");
        } else if (isInterestGroupJoined(joinedKey)) {
            logger.error("Requested to join an interest group that is already joined to.");
        }

        return joined;
    }

    public void publishWorkProduct(String interestGroupID, String wpID, String wpType, String wp) {

        logger.debug("publishWorkProduct: interestGroupID=" + interestGroupID + "  wpID=" + wpID +
                     " wpType=" + wpType);

        final InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);

        if (interestGroup != null) {
            final String wpTypeNode = wpType + "_" + interestGroupID;

            if (interestGroup.workProductTypes.contains(wpType)) {
                // remove work product publication first if it exists in case this is an update
                try {
                    if (interestManager.retrieveNodeItem(interestGroup.interestGroupPubsubService,
                                                         wpTypeNode,
                                                         wpID) != null) {
                        try {
                            interestManager.removeItem(interestGroup.interestGroupPubsubService,
                                                       wpTypeNode,
                                                       wpID);
                        } catch (final Exception e) {
                            logger.error("publishWorkProduct: removeItem: productID: " + wpID +
                                         ": " + e.getMessage());
                        }
                    } else {
                        logger.debug("publishWorkProduct: productID: " + wpID + " not existed");
                    }
                } catch (final Exception e) {
                    logger.error("publishWorkProduct: retrieveNodeItem: productID: " + wpID + ": " +
                        e.getMessage());
                    return;
                }
            } else {
                // create a new product type node
                interestManager.addNode(interestGroup.interestGroupPubsubService,
                                        interestGroup.interestGroupNode,
                                        wpTypeNode,
                                        NODE_ITEM_TYPE.ITEM_LIST,
                                        "");

                // update the subscription map
                interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

                // add new work product type
                interestGroup.workProductTypes.add(wpType);

                // tell the join cores of the new product type if this a shareAllTypes incident
                // Note: this is needed since we no longer subscribe to the incident node (conflict
                // error), but to all product types
                // This will not be needed if the conflict error is resolved
                final Set<String> joinedCoreJIDs = interestGroup.joinedCoreJIDMap.keySet();
                for (final String joinedCoreJID : joinedCoreJIDs) {
                    if (interestGroup.joinedCoreJIDMap.get(joinedCoreJID) == true) {
                        logger.debug("publishWorkProduct: remoteCoreJID: " +
                            joinedCoreJID +
                            " is " +
                            (getCoreConnection().isCoreOnline(joinedCoreJID) ? " online" : "offline"));
                        // this incident was shared for all work product types with the joined core
                        interestManager.sendUpdateJoinMessage(joinedCoreJID,
                                                              interestGroupID,
                                                              wpType);
                    }
                }
            }

            final String xmlItem = PubSubIQFactory.createItemXML(wp, wpID);
            // publish interest group work product to node
            // log.debug("publishWorkProduct: publish [" + xmlItem + "] to node " + wpTypeNode);

            interestManager.publishToNode(interestGroup.interestGroupPubsubService,
                                          wpTypeNode,
                                          xmlItem);

        } else {
            logger.error("Unable to find interestGroup:[" + interestGroupID + "] in map");
            // throw exception
        }
    }

    // public void publishInterestGroup(String interestGroupID, String interestGroupWPID,
    // String interestGroupWP, String productIDs[]) {
    // InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
    // if (interestGroup != null) {
    // interestGroup.workProductIDs = productIDs;
    // }
    // publishWorkProduct(interestGroupID, interestGroupWPID, interestGroupWP);
    // }

    public synchronized void removeAllFromFailedJoins(ArrayList<InterestGroup> groupsToRemove) {

        failedJoins.removeAll(groupsToRemove);
    }

    public synchronized void removeJoiningCoreFromInterestGroup(String interestGroupID, String core) {

        logger.debug("removeJoiningCoreFromInterestGroup: remove JID: " + core +
                     " from IGID's list " + interestGroupID);
        if (joiningCores.containsKey(interestGroupID) &&
            joiningCores.get(interestGroupID).contains(core)) {
            joiningCores.get(interestGroupID).remove(core);
        }
    }

    public void requestJoinedPublishProduct(String interestGroupID,
                                            String owningCore,
                                            String productId,
                                            String productType,
                                            String act,
                                            String userID,
                                            String product) {

        interestManager.sendJoinedPublishProductRequestMessage(interestGroupID,
                                                               owningCore,
                                                               productId,
                                                               productType,
                                                               act,
                                                               userID,
                                                               product);
    }

    /**
     * Called when a request is received from the owner of an interest group that this core should
     * resign from the interest group. This method should not be called on a core that owns the
     * interest group.
     *
     * @param interestGroupID
     *            UUID of the interest group
     * @param to
     *            owning core
     * @param packetId
     *            id attribute of resign request message
     * @param xml
     *            XML text of original resign message
     * @throws IllegalArgumentException
     */
    public void resignFromInterestGroup(String interestGroupID,
                                        String to,
                                        String packetId,
                                        String xml) throws IllegalArgumentException {

        // Make sure the interest group name is not empty or null
        if ((interestGroupID == null) || (interestGroupID.length() == 0)) {
            throw new IllegalArgumentException("InterestGroup UUID cannot be empty");
        }

        final InterestGroup interestGroup = joinedInterestGroups.get(interestGroupID);
        if (interestGroup == null) {
            throw new IllegalArgumentException("InterestGroup with uuid " + interestGroupID +
                " not found.");
        }

        logger.info("resignFromInterestGroup resigning from interest group ID=" + interestGroupID);

        // Unsubscribe to all nodes for this interest group
        interestManager.unsubscribeAllForInterestGroup(interestGroup.interestGroupPubsubService,
                                                       interestGroupID);

        // remove from mangement
        synchronized (this) {
            joinedInterestGroups.remove(interestGroupID);
        }

        // Let owner know that we have resigned
        // (String owningJID, String uuid)
        final IQ msg = InterestGrptManagementIQFactory.createResignConfirmMessage(to, packetId, xml);
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending resign from interest group message: " + e.getMessage());
            logger.debug("resignFromInterestGroup message: " + msg.toXML());
        }

    }

    public void restoreJoinedInterestGroup(InterestGroup interestGroup, String ownerPropString) {

        logger.info("restoreJoinedInterestGroup: interestGroupID=" + interestGroup.interestGroupID +
                    " interestGroupType=" + interestGroup.interestGroupType);

        if (interestGroup != null) {
            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            String wpTypeNode = "";
            boolean connectionRestoreSuccessful = true;
            try {

                // subscribe to the child nodes (specified workProductType's) at the owning
                // core

                interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

                for (final String wpType : interestGroup.workProductTypes) {
                    wpTypeNode = wpType + "_" + interestGroup.interestGroupID;
                    logger.debug("restoreJoinedInterestGroup - subscribing to node:" + wpTypeNode);
                    interestManager.subscribeToNode(interestGroup.interestGroupPubsubService,
                                                    wpTypeNode);
                }

                // Add the joined interest group
                interestGroup.state = CORE_STATUS.JOINED;
                final String joinedKey = interestGroup.interestGroupID;
                joinedInterestGroups.put(joinedKey, interestGroup);
            } catch (final XMPPException e) {
                logger.error("restoreInterestGroup: error subscribing to ownner's node " +
                    wpTypeNode);
                if (e.getXMPPError() != null) {
                    logger.error("  message: " + e.getXMPPError().getMessage());
                    logger.error("     code: " + e.getXMPPError().getCode());
                    logger.error("     type: " + e.getXMPPError().getType());
                    logger.error("condition: " + e.getXMPPError().getCondition());
                } else {
                    logger.error("  null XMPPP error");
                }
                connectionRestoreSuccessful = false;

            } catch (final Exception e) {
                logger.error("restoreInterestGroup: error subscribing to owner's node " +
                    wpTypeNode);
                e.printStackTrace();
                connectionRestoreSuccessful = false;
            }
            if (!connectionRestoreSuccessful) {
                // notify InterestGroupManagementComponent to delete the restored interest
                // group since we can't subscribe, i.e. the
                // interest group may have been deleted when we were down
                final DeleteJoinedInterestGroupMessage message = new DeleteJoinedInterestGroupMessage();
                message.setInterestGroupID(interestGroup.interestGroupID);
                final Message<DeleteJoinedInterestGroupMessage> notification = new GenericMessage<DeleteJoinedInterestGroupMessage>(message);
                deleteJoinedInterestGroupNotificationChannel.send(notification);
            }
        }
    }

    public void restoreOwnedInterestGroup(InterestGroup interestGroup) {

        logger.info("restoreOwnedInterestGroup: interestGroupID=" + interestGroup.interestGroupID +
                    " interestGroupType=" + interestGroup.interestGroupType);

        if (interestGroup != null) {
            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            // Here the incident's sharing status is either Shared or None

            // add to interest group status map
            // log.debug("restoreOwnedInterestGroup: add interest group to map");
            interestGroup.state = CORE_STATUS.OWNED;

            ownedInterestGroups.put(interestGroup.interestGroupID, interestGroup);

            addToJoiningCoresList(interestGroup);

            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // RDW - need to figure out what to do when the return from addFolder is null
            // i.e. the creation of the node failed.
            if (interestManager.addFolder(interestGroup.interestGroupPubsubService,
                                          coreConnection.getInterestGroupRoot(),
                                          interestGroup.interestGroupNode) == null) {
                logger.error("cannot create node for restored owned interest group: " +
                    interestGroup.interestGroupNode);
            }

            // update the subscription map
            interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

        }
    }

    public void restoreSharedInterestGroup(InterestGroup interestGroup, Set<String> sharedCoreList) {

        logger.info("restoreSharedInterestGroup: interestGroupID=" + interestGroup.interestGroupID +
                    " interestGroupType=" + interestGroup.interestGroupType);

        if (interestGroup != null) {
            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            // Here the incident's sharing status is either Shared or None

            interestGroup.state = CORE_STATUS.JOIN_IN_PROGRESS;

            ownedInterestGroups.put(interestGroup.interestGroupID, interestGroup);

            addToJoiningCoresList(interestGroup);

            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // RDW - need to figure out what to do when the return from addFolder is null
            // i.e. the creation of the node failed.
            interestManager.addFolder(interestGroup.interestGroupPubsubService,
                                      coreConnection.getInterestGroupRoot(),
                                      interestGroup.interestGroupNode);

            // update the subscription map
            interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

            synchronized (this) {
                for (final String joinedCore : sharedCoreList) {
                    final List<String> joiningCoreList = getJoiningCoresList(interestGroup.interestGroupID);
                    if (joiningCoreList != null) {
                        addToJoiningCoresList(interestGroup);
                    }
                    addJoiningCoreToInterestGroup(interestGroup, joinedCore);

                    // Don't worry about attempting to share again
                    // we're only trying to re-populate internal data and re-producing events

                }
            }

        }
    }

    public void retryFailedJoins() {

        final ArrayList<InterestGroup> successfulJoins = new ArrayList<InterestGroup>();
        final List<InterestGroup> failures = getFailedJoins();
        if ((failures != null) && (failures.size() > 0)) {
            for (final InterestGroup ig : failures) {
                logger.info("Retrying failed join to " + ig.interestGroupID);
                try {
                    joinInterestGroup(ig, "", ig.interestGroupInfo);
                    successfulJoins.add(ig);
                } catch (final XMPPException e) {
                    logger.error("Error retrying join for " + ig.interestGroupID);
                }
            }
        }
        if (successfulJoins.size() > 0) {
            removeAllFromFailedJoins(successfulJoins);
        }
    }

    public void sendProductPublicationStatus(String requestingCore, String userID, String status) {

        logger.debug("sendProductPublicationStatus: coreJID: " + requestingCore + ", userID: " +
            userID + ", status: " + status);
        interestManager.sendProductPublicationStatusMessage(requestingCore, userID, status);
    }

    public void setCoreConnection(CoreConnection c) {

        coreConnection = c;
    }

    public void setDeleteInterestGroupSharedFromRemoteCoreChannel(MessageChannel deleteInterestGroupSharedFromRemoteCoreChannel) {

        this.deleteInterestGroupSharedFromRemoteCoreChannel = deleteInterestGroupSharedFromRemoteCoreChannel;
    }

    public void setDeleteJoinedInterestGroupNotificationChannel(MessageChannel deleteJoinedInterestGroupNotificationChannel) {

        this.deleteJoinedInterestGroupNotificationChannel = deleteJoinedInterestGroupNotificationChannel;
    }

    public void setDeleteJoinedProductNotificationChannel(MessageChannel deleteJoinedProductNotificationChannel) {

        this.deleteJoinedProductNotificationChannel = deleteJoinedProductNotificationChannel;
    }

    public void setDisseminationManagerChannel(MessageChannel channel) {

        disseminationManagerChannel = channel;
    }

    public void setInterestManager(InterestManager im) {

        interestManager = im;
    }

    public void setJoinedInterestGroupNotificationChannel(MessageChannel joinedInterestGroupNotificationChannel) {

        this.joinedInterestGroupNotificationChannel = joinedInterestGroupNotificationChannel;
    }

    public void setJoinedPublishProductNotificationChannel(MessageChannel joinedPublishProductNotificationChannel) {

        this.joinedPublishProductNotificationChannel = joinedPublishProductNotificationChannel;
    }

    public void setLdapUtil(LdapUtil ldapUtil) {

        this.ldapUtil = ldapUtil;
    }

    public void setProductPublicationStatusNotificationChannel(MessageChannel productPublicationStatusNotificationChannel) {

        this.productPublicationStatusNotificationChannel = productPublicationStatusNotificationChannel;
    }

    public void shareInterestGroup(ShareInterestGroupMessage message)
        throws IllegalArgumentException, IllegalStateException {

        logger.info("shareInterestGroup: interestGroupID: " + message.getInterestGroupID() +
                    " with remoteCoreJID: " + message.getRemoteCore());

        for (final String workProductType : message.getWorkProductTypesToShare()) {
            logger.info("shareInterestGroup: type:" + workProductType);
        }

        final InterestGroup interestGroup = ownedInterestGroups.get(message.getInterestGroupID());
        final List<String> joiningCoreList = getJoiningCoresList(message.getInterestGroupID());

        if (interestGroup != null) {
            logger.debug("remoteCore in joiningCores: " +
                joiningCoreList.contains(message.getRemoteCore()));
            logger.debug("interestGroupState: " + interestGroup.state);
        } else {
            logger.error("null interestGroup");
        }

        if (interestGroup == null) {
            logger.error("shareInterestGroup: Unable to find interestGroup:[" +
                message.getInterestGroupID() + "] in ownedInterestGroups map");
            throw new IllegalArgumentException("shareInterestGroup: unable to find: " +
                message.getInterestGroupID() + " in map", null);
        }

        if (joiningCoreList == null) {
            logger.error("Unable to find joinedCores for: " + message.getInterestGroupID());
            return;
        }

        if ((joiningCoreList.contains(message.getRemoteCore()) == false) ||
            ((interestGroup.state != CORE_STATUS.JOINED) && (interestGroup.state != CORE_STATUS.JOIN_IN_PROGRESS))) {

            if (coreConnection.isCoreOnline(message.getRemoteCore()) == false) {
                logger.warn("shareInterestGroup: remoteJID: " + message.getRemoteCore() +
                    " is offline, queue the request \n");
                // String messageString = SerializerUtil.serialize(message);
                // getQueuedMessageDAO().saveMessage(message.getRemoteCore(), messageString);
                return;
            }

            // save the interest group info
            interestGroup.interestGroupInfo = message.getInterestGroupInfo();

            interestGroup.state = CORE_STATUS.JOIN_IN_PROGRESS;

            Boolean shareAllTypes;
            if (message.getWorkProductTypesToShare().size() == 0) {
                // tell remote core to subscribe to all types
                shareAllTypes = true;
                logger.debug("shareInterestGroup: share all wpTypes");
                for (final String wpType : interestGroup.workProductTypes) {
                    message.getWorkProductTypesToShare().add(wpType);
                }
            } else {
                // verify that all the specified product types have been received for the
                // interest group
                shareAllTypes = false;
                for (final String wpType : message.getWorkProductTypesToShare()) {
                    if (!interestGroup.workProductTypes.contains(wpType)) {
                        throw new IllegalArgumentException("shareInterestGroup: unknown specified to-share work product  type : " +
                            wpType +
                            " for interest group " +
                            message.getInterestGroupID(),
                            null);
                    }
                }
            }

            // add to list of cores that are in the process of joining
            addJoiningCoreToInterestGroup(interestGroup, message.getRemoteCore());

            logger.debug("===> sending join message with types to share:");
            for (final String workProductType : message.getWorkProductTypesToShare()) {
                logger.info("===> type:" + workProductType);
            }

            // log.info("shareInterestGroup - call sendJoinMessage");
            interestManager.sendJoinMessage(message.getRemoteCore(),
                                            interestGroup,
                                            message.getInterestGroupInfo(),
                                            message.getWorkProductTypesToShare());

            interestGroup.joinedCoreJIDMap.put(message.getRemoteCore(), shareAllTypes);
        }
    }

    public void unJoinedCore(String joinedCoreName) {

        logger.debug("unJoinedCore: " + joinedCoreName);
        final Collection<InterestGroup> interestGroups = ownedInterestGroups.values();
        for (final InterestGroup interestGroup : interestGroups) {
            final boolean shared = interestGroup.joinedCoreJIDMap.remove(joinedCoreName);
            logger.debug("unJoinedCore: IGID: " + interestGroup.interestGroupID + " does " +
                (shared ? "" : "NOT") + " shared with " + joinedCoreName);
        }
    }

    public void updateJoinInterestGroup(String interestGroupID, String productType) {

        logger.info("updateJoinInterestGroup " + interestGroupID);

        final InterestGroup interestGroup = joinedInterestGroups.get(interestGroupID);
        if (interestGroup != null) {
            final String wpTypeNode = productType + "_" + interestGroup.interestGroupID;
            try {
                // InterestManager ownerIM = getOwnerInterestManager(interestGroup.ownerProps);
                // ownerIM.subscribeToOwnersNode(wpTypeNode, interestGroup.ownerProps);
                // RDW
                interestManager.subscribeToNode(interestGroup.interestGroupPubsubService,
                                                wpTypeNode);
            } catch (final Exception e) {
                logger.error("updateJoinInterestGroup: Error subscribing to owner's node " +
                    wpTypeNode);
                return;
            }
            interestGroup.workProductTypes.add(productType);
        }
    }

}
