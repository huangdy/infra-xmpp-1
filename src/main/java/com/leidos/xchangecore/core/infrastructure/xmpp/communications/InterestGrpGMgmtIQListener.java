package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;

import com.leidos.xchangecore.core.infrastructure.messages.DeleteJoinedProductMessage;
import com.leidos.xchangecore.core.infrastructure.messages.JoinedPublishProductRequestMessage;
import com.leidos.xchangecore.core.infrastructure.messages.ProductPublicationStatusNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.InterestGroupManager.CORE_STATUS;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.util.XmppUtils;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt.InterestGrptManagementIQFactory;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.ArbitraryIQ;

public class InterestGrpGMgmtIQListener
    implements PacketListener {

    private final Logger log = Logger.getLogger(this.getClass());

    private final InterestGroupManager interestGroupManager;
    private final Pattern uuidPattern = Pattern.compile("uuid=[\"']([\\w-]+?)[\"']");
    private final Pattern ownerPattern = Pattern.compile("owner=[\"'](.+?)[\"']");
    private final Pattern interestGroupTypePattern = Pattern.compile("interestGroupType=[\"'](.+?)[\"']");
    private final Pattern corePattern = Pattern.compile("core=[\"'](.+?)[\"']");
    private final Pattern joinPattern = Pattern.compile("<join");
    private final Pattern resignPattern = Pattern.compile("<resign ");
    private final Pattern resignRequestPattern = Pattern.compile("<resign-request ");
    private final Pattern resignedPattern = Pattern.compile("<resigned ");
    private final Pattern readyPattern = Pattern.compile("<ready");
    private final Pattern statusElementPattern = Pattern.compile("<status>([\\w-]+)</status>");
    private final Pattern alreadyJoinedPattern = Pattern.compile("already-joined");
    private final Pattern joinedPublishPattern = Pattern.compile("<requestJoinedPublish");
    private final Pattern productPublicationPattern = Pattern.compile("<productPublicationStatus");
    private final Pattern cleanupJoinedInterestGroupPattern = Pattern.compile("<cleanupJoinedInterestGroup");
    private final Pattern deleteJoinedInterestGroupPattern = Pattern.compile("<deleteJoinedInterestGroup");
    private final Pattern deleteJoinedProductPattern = Pattern.compile("<deleteJoinedProduct");
    private final Pattern updateJoinPattern = Pattern.compile("<updateJoin");
    private final Pattern productPayloadElementPattern = Pattern.compile("<ProductPayload>(.+?)</ProductPayload>",
        Pattern.DOTALL | Pattern.MULTILINE);
    private final Pattern publicationStatusElementPattern = Pattern.compile("<ProductPublicationStatus>(.+?)</ProductPublicationStatus>",
        Pattern.DOTALL | Pattern.MULTILINE);
    private final Pattern ItemElementPattern = Pattern.compile("<item>(.+?)</item>",
        Pattern.DOTALL | Pattern.MULTILINE);

    public InterestGrpGMgmtIQListener(InterestGroupManager instance) {

        interestGroupManager = instance;
    }

    private void doCleanupJoinedInterestGroup(String from, String packetId, String xml) {

        log.debug("doCleanupJoinedInterestGroup: packetID: " + packetId + " from:" + from);

        String interestGroupID = null;
        Pattern pattern = Pattern.compile("uuid=[\"'](.+?)[\"']");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            interestGroupID = matcher.group(1);
            log.debug("doCleanupJoinedInterestGroup: IGID: " + interestGroupID);
        } else {
            log.error("doCleanupJoinedInterestGroup: unable to extract  interestGroupID(uuid) from the received message.");
            return;
        }

        // notify the Interest Group Management Component of the deleted interest group
        interestGroupManager.cleanupInterestGroupSharedFromRemoteCore(from, interestGroupID);
    }

    private void doDeleteJoinedInterestGroup(String from, String packetId, String xml) {

        log.debug("doDeleteJoinedInterestGroup: packetID: " + packetId + " from:" + from);

        String interestGroupID = null;
        Pattern pattern = Pattern.compile("uuid=[\"'](.+?)[\"']");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            interestGroupID = matcher.group(1);
            log.debug("doDeleteJoinedInterestGroup: IGID: " + interestGroupID);
        } else {
            log.error("doDeleteJoinedInterestGroup: unable to extract  interestGroupID(uuid) from the received message.");
            return;
        }

        // notify the Interest Group Management Component of the deleted interest group
        interestGroupManager.deleteJoinedInterestGroup(interestGroupID);
    }

    private void doDeleteJoinedProduct(String from, String packetId, String xml) {

        log.debug("doDeleteJoinedProduct: packetID: " + packetId + " from: " + from);

        String productID = null;
        Pattern pattern = Pattern.compile("uuid=[\"'](.+?)[\"']");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            productID = matcher.group(1);
        } else {
            log.error("doDeleteJoinedProduct: unable to extract  proiducID(uuid) from the received message.");
            return;
        }

        // notify the Interest Group Management Component of the deleted interest group
        DeleteJoinedProductMessage message = new DeleteJoinedProductMessage();
        message.setProductID(productID);
        Message<DeleteJoinedProductMessage> notification = new GenericMessage<DeleteJoinedProductMessage>(message);
        interestGroupManager.getDeleteJoinedProductNotificationChannel().send(notification);
    }

    /**
     * The receiving core is requested to join the Interest group. This method should only be
     * processed by a core that does not own the Interest group referenced in the message.
     *
     * @param from
     *            JID of owning core
     * @param packetId
     *            XMPP packet id
     * @param xml
     *            XML text of the message
     */
    private void doJoin(String from, String packetId, String xml) {

        log.debug("doJoin: packetID=" + packetId);

        String interestGroupID = "";
        Matcher m = uuidPattern.matcher(xml);
        if (m.find()) {
            interestGroupID = m.group(1);
        } else {
            log.error("doJoin: Can't find interestGroupID in join message");
        }

        String owner = "";
        m = ownerPattern.matcher(xml);
        if (m.find()) {
            owner = m.group(1);
        } else {
            log.error("doJoin: Can't find owner in join message");
        }

        String interestGroupType = "";
        m = interestGroupTypePattern.matcher(xml);
        if (m.find()) {
            interestGroupType = m.group(1);
        } else {
            log.error("doJoin: Can't find interestGroupWPID  in join message");
        }

        String coreJID = interestGroupManager.getCoreConnection().getJID();
        interestGroupManager.getCoreConnection().getCoreNameFromJID(coreJID);
        // String joinedKey = interestGroupID + "." + coreName;
        String joinedKey = interestGroupID;

        // Check if we are already joined to this interest group
        if (interestGroupManager.isInterestGroupJoined(joinedKey)) {

            // return an already joined result
            log.error(" IncdMgmtIQListener:doJoin: ALREADY JOINED");
            IQ msg = InterestGrptManagementIQFactory.alreadyJoinedMessage(from, packetId, xml);
            // log.debug(msg.toXML());
            try {
                interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
            } catch (XMPPException e) {
                log.error("Error sending already joined result: " + e.getMessage());
                log.debug("bad doJoin message: " + msg.toXML());
            }
        } else if (interestGroupManager.isInterestGroupOwned(interestGroupID)) {
            // return an error that we own the interest group
            log.error(" IncdMgmtIQListener:doJoin: ALREADY OWNED");
            IQ msg = InterestGrptManagementIQFactory.alreadyOwnedError(from, packetId, xml);
            // log.debug(msg.toXML());
            try {
                interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
            } catch (XMPPException e) {
                log.error("Error sending we own IG result: " + e.getMessage());
                log.debug("bad doJoin message: " + msg.toXML());
            }
        } else {
            // Acknowledge the join
            IQ iq = InterestGrptManagementIQFactory.acknowledgeJoinMessage(from, packetId);
            log.debug("doJoin: send AcknowledgeJoinMessage:" + iq.toXML());
            try {
                interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(iq);
            } catch (XMPPException e1) {
                log.error("Error sending we join acknowledgement result: " + e1.getMessage());
                log.debug("bad doJoin message: " + iq.toXML());
            }

            // Join the interest group
            // log.debug(
            // " InterestGroupManagementListener:doJoin: calling interestGroupMangaer.joinInterestGroup");

            // Get the owners connection properties
            // RDW - should be able to remove this at some point
            StringBuffer xmlProps = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">");
            String temp = "";
            if (xml.indexOf("<properties") != -1) {
                temp = xml.substring(xml.indexOf("<properties"), xml.indexOf("</properties") + 13);
                xmlProps.append(temp);
            }

            // Get the interest group document

            List<String> workProductTypesToShare = new ArrayList<String>();
            if (xml.indexOf("<workProductTypesToShare/>") == -1) {
                // if not an empty workProductTypes list
                if (xml.indexOf("<workProductTypesToShare>") != -1) {
                    String productTypes = xml.substring(xml.indexOf("<workProductTypesToShare>") + 25,
                        xml.indexOf("</workProductTypesToShare>"));
                    // log.info("InterestGroupMgmtIQListener:doJoin: workProductTypesToShare=["
                    // + productTypes + "]");
                    // TODO: add types in array

                    Matcher matcher = ItemElementPattern.matcher(productTypes);
                    while (matcher.find()) {
                        // log.info("InterestGroupMgmtIQListener:doJoin ======> Found type:"
                        // + matcher.group(1));
                        workProductTypesToShare.add(matcher.group(1));
                    }

                } else {
                    log.error(" InterestGroupMgmtIQListener:doJoin: ERROR no productTypes " + xml);
                }
            }

            String interestGroupInfo = "";
            if (xml.indexOf("<info>") != -1) {
                interestGroupInfo = xml.substring(xml.indexOf("<info>") + 6, xml.indexOf("</info>"));
            }

            try {

                InterestGroup interestGroup = new InterestGroup();
                interestGroup.interestGroupID = interestGroupID;
                interestGroup.interestGroupType = interestGroupType;
                interestGroup.interestGroupOwner = owner;
                interestGroup.workProductTypes = workProductTypesToShare;
                interestGroup.interestGroupPubsubService = XmppUtils.getPubsubServiceFromJID("pubsub",
                    owner);

                log.debug("doJoin:  interestGroupID=" + interestGroup.interestGroupID);
                if (!interestGroupManager.joinInterestGroup(interestGroup,
                    xmlProps.toString(),
                    interestGroupInfo)) {
                    log.debug(" IncdMgmtIQListener:doJoin: error joining interest group");
                    IQ msg = InterestGrptManagementIQFactory.coreError(from, packetId, xml);
                    log.debug(msg.toXML());
                    interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
                }

            } catch (XMPPException e) {
                log.debug(" IncdMgmtIQListener:doJoin: XMPP exception joining interest group");
                IQ msg = InterestGrptManagementIQFactory.coreError(from, packetId, xml);
                log.debug("Core XMPP error: " + msg.toXML());
                if (e.getXMPPError() == null) {
                    log.error("XMPPError is null so sending plain error");
                    interestGroupManager.getCoreConnection().sendPacket(msg);
                } else {
                    IQ errorIQ = ArbitraryIQ.createResultError(msg, e.getXMPPError());
                    log.debug("Have XMPPError so sending error: " + errorIQ.toXML());
                    interestGroupManager.getCoreConnection().sendPacket(errorIQ);
                }
            } catch (IllegalArgumentException e) {
                log.debug(" IncdMgmtIQListener:doJoin: IllegalArgumentException exception caught - CORE ERROR");
                IQ msg = InterestGrptManagementIQFactory.coreError(from, packetId, xml);
                log.debug(msg.toXML());
                interestGroupManager.getCoreConnection().sendPacket(msg);
            }
        }
    }

    /**
     * The receiving core is getting an acknowledgement that a core it requested to join an Interest
     * group has received the join request. Only cores that own an Interest group should get to this
     * method.
     *
     * @param from
     *            JID of core that is acknowledging the join request
     * @param packetId
     *            - XMPP packet id from the original join request
     * @param xml
     *            XML text of the message
     */
    private void doJoinAcknowledge(String from, String packetId, String xml) {

        log.debug("doJoinAcknowledge: ");
        String coreName = interestGroupManager.getCoreConnection().getCoreNameFromJID(from);
        String interestGroupID = "";
        Matcher m = uuidPattern.matcher(xml);
        if (m.find()) {
            interestGroupID = m.group(1);
            if (interestGroupManager.isCoreJoining(coreName, interestGroupID)) {
                // TODO: may want to set a timer to reset this flag
                if (interestGroupManager.isInterestGroupOwned(interestGroupID)) {
                    InterestGroup interestGroup = interestGroupManager.getOwnedInterestGroup(interestGroupID);
                    if (interestGroup != null) {
                        interestGroup.suspendUpdateProcessing(interestGroupID);
                    } else {
                        log.error("doJoinAcknowledge target interest group deleted");
                    }
                }
            } else {
                log.error("doJoinAcknowledge unknown uuid " + interestGroupID);
            }
        } else {
            log.error("doJoinAcknowledge: Can't find uuid in join message");
        }
    }

    /**
     * The receiving core receives a request from a joined core to either publish a new work product
     * or update an existing work product that is associated with a shared interest group.
     *
     * @param from
     *            JID of owning core
     * @param packetId
     *            XMPP packet id
     * @param xml
     *            XML text of the message
     */
    private void doJoinedPublishRequest(String from, String packetId, String xml) {

        log.debug("doJoinedPublishRequest: packetID=" + packetId + " from=" + from);

        String interestGroupId = null;
        Pattern pattern = Pattern.compile("interestGroupId=[\"'](.+?)[\"']");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            interestGroupId = matcher.group(1);
        } else {
            log.error("doJoinedPublishRequest: unable to extract  interestGroupId from the received message.");
            return;
        }

        String productId = null;
        pattern = Pattern.compile("productId=[\"'](.+?)[\"']");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            productId = matcher.group(1);
        } else {
            log.error("doJoinedPublishRequest: unable to extract  productId from the received message.");
            return;
        }

        String productType = null;
        pattern = Pattern.compile("productType=[\"'](.+?)[\"']");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            productType = matcher.group(1);
        } else {
            log.error("doJoinedPublishRequest: unable to extract  productType from the received message.");
            return;
        }

        String act = null;
        pattern = Pattern.compile("act=[\"'](.+?)[\"']");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            act = matcher.group(1);
        } else {
            log.error("doJoinedPublishRequest: unable to extract  access control token from the received message.");
            return;
        }

        String userID = null;
        pattern = Pattern.compile("userID=[\"'](.+?)[\"']");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            userID = matcher.group(1);
        } else {
            log.error("doJoinedPublishRequest: unable to extract  user ID from the received message.");
            return;
        }

        String owningCore = null;
        pattern = Pattern.compile("owningCore=[\"'](.+?)[\"']");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            owningCore = matcher.group(1);
        } else {
            log.error("doJoinedPublishRequest: unable to extract  owningCore from the received message.");
            return;
        }

        String requestingCore = null;
        pattern = Pattern.compile("requestingCore=[\"'](.+?)[\"']");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            requestingCore = matcher.group(1);
        } else {
            log.error("doJoinedPublishRequest: unable to extract  requestingCore from the received message.");
            return;
        }

        String workProductString = null;
        Matcher m = productPayloadElementPattern.matcher(xml);
        if (m.find()) {
            workProductString = m.group(1);
            log.debug("====> wp=[" + workProductString + "]");
        } else {
            log.error("doJoinedPublishRequest: unable to extract work product from the received message.");
            return;
        }

        // send the publish request to the interest group owning core
        JoinedPublishProductRequestMessage msg = new JoinedPublishProductRequestMessage();
        msg.setInterestGroupId(interestGroupId);
        msg.setOwningCore(owningCore);
        msg.setRequestingCore(requestingCore);
        msg.setProductId(productId);
        msg.setProductType(productType);
        msg.setWorkProduct(workProductString);
        msg.setAct(act);
        msg.setUserID(userID);
        Message<JoinedPublishProductRequestMessage> message = new GenericMessage<JoinedPublishProductRequestMessage>(msg);
        log.debug("===> doJoinedPublishRequest: sending JoinedPublishProductRequestMessage");
        interestGroupManager.getJoinedPublishProductNotificationChannel().send(message);

    }

    /**
     * The receiving core is getting told that the core that it requested to join an interest group
     * has retrieved the current state of the Interest group and is ready to receive updates. Only
     * cores that own an Interest group should get to this method.
     *
     * @param from
     *            JID of core that has joined and is ready
     * @param xml
     *            XML text of the message
     */
    private void doJoinReady(String from, String xml) {

        log.debug("doJoinReady: ");
        String interestGroupID = "";
        Matcher m = uuidPattern.matcher(xml);
        if (m.find()) {
            interestGroupID = m.group(1);
            if (interestGroupManager.isInterestGroupOwned(interestGroupID)) {
                // TODO: make sure that all cores in the process of joining are done
                // before procesing and reset
                InterestGroup interestGroup = interestGroupManager.getOwnedInterestGroup(interestGroupID);
                synchronized (interestGroupManager.getProcessSuspendedUpdatesLock()) {
                    if (interestGroup != null) {
                        interestGroup.processSuspendedUpdates();
                        interestGroup.resetSuspendUpdateProcessing(interestGroupID);
                    } else {
                        log.error("doJoinReady: target interest group deleted");
                    }
                }
                // Add core to the interest groups bookkeeping
                String coreName = interestGroupManager.getCoreConnection().getCoreNameFromJID(from);

                interestGroup.addCoreToInterestGroupAfterJoin(interestGroupID, coreName);

            }
        } else {
            log.error("doJoinReady: Can't find interest groupID in join message");
        }
    }

    private void doNotifyProductPublicationStatus(String from, String packetId, String xml) {

        log.debug("doNotifyProductPublicationStatus: packetID=" + packetId + " from:" + from);

        String userID = null;
        Pattern pattern = Pattern.compile("userID=[\"'](.+?)[\"']");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            userID = matcher.group(1);
        } else {
            log.error("doNotifyProductPublicationStatus: unable to extract  userID from the received message.");
            return;
        }

        String statusString = null;
        Matcher m = publicationStatusElementPattern.matcher(xml);
        if (m.find()) {
            statusString = m.group(1);
            log.debug("====> status=[" + statusString + "]");
        } else {
            log.error("doJoinedPublishRequest: unable to extract publication status from the received message.");
            return;
        }

        // send the publish request to the interest group owning core
        ProductPublicationStatusNotificationMessage msg = new ProductPublicationStatusNotificationMessage();
        msg.setUserID(userID);
        msg.setOwningCore(from);
        msg.setStatus(statusString);
        Message<ProductPublicationStatusNotificationMessage> message = new GenericMessage<ProductPublicationStatusNotificationMessage>(msg);
        log.debug("===> doJoinedPublishRequest: sending JoinedPublishProductRequestMessage");
        interestGroupManager.getProductPublicationStatusNotificationChannel().send(message);
    }

    /**
     * The receiving core is a joined core and is being asked to resign from the interest group.
     *
     * @param from
     *            JID of the core that owns the interest group
     * @param xml
     *            XML text of the message
     */
    private void doResign(String from, String packetId, String xml) {

        log.debug("doResign: ");
        String uuid = "";
        Matcher m = uuidPattern.matcher(xml);
        if (m.find()) {
            uuid = m.group(1);
            if (interestGroupManager.isInterestGroupJoined(uuid)) {
                // TODO: need synchronization here
                // or where we remove from the joinedInterestGroups list
                try {
                    interestGroupManager.resignFromInterestGroup(uuid, from, packetId, xml);
                } catch (IllegalArgumentException e) {
                    log.error("doResign exception: " + e.getMessage());
                }
            }
        } else {
            log.error("doResign: Can't find uuid in join message");
        }
    }

    /**
     * The receiving core is the owner of the interest group and is receiving a confirmation that
     * the joined core (from) has finished resigning from the interest group.
     *
     * @param from
     *            core that has resigned
     * @param packetId
     *            id of
     * @param xml
     */
    private void doResignAcknowledge(String from, String packetId, String xml) {

        log.debug("doResignAcknowledge: ");
        // Add to joined cores list
        interestGroupManager.getCoreConnection().getCoreNameFromJID(from);
        String interestGroupID = "";
        Matcher m = uuidPattern.matcher(xml);
        if (m.find()) {
            interestGroupID = m.group(1);
            if (interestGroupManager.isInterestGroupOwned(interestGroupID)) {
                // TODO:
                // removeCoreFromInterestGroup(interestGroupManager.getJoinedInterestGroup(interestGroupID,
                // coreName));
            } else {
                log.error("doResignAcknowledge unknown uuid " + interestGroupID);
            }
        } else {
            log.error("doResignAcknowledge: Can't find uuid in join message");
        }
    }

    /**
     * The receiving core is the owner and is being asked to resign a joined core from the interest
     * group.
     *
     * @param from
     *            JID of the core that owns the interest group
     * @param xml
     *            XML text of the message
     */
    private void doResignRequest(String from, String packetId, String xml) {

        log.debug("doResignRequest: from: " + from + ", packetId: " + packetId + ", [" + xml + "]");
        String interestGroupID = "";
        Matcher m = uuidPattern.matcher(xml);
        if (m.find()) {
            interestGroupID = m.group(1);

            // String coreName = "";
            m = corePattern.matcher(xml);
            if (m.find()) {
                String coreJID = m.group(1);
                interestGroupManager.getCoreConnection().getCoreNameFromJID(coreJID);
                try {
                    if (interestGroupManager.isInterestGroupOwned(interestGroupID)) {
                        // TODO
                        // requestCoreResign(
                        // interestGroupManager.getOwnedInterestGroup(interestGroupID).interestGroupAtomEntryText,
                        // coreName, interestGroupID);
                    } else {
                        interestGroupManager.getInterestManager().sendResignMessage(coreJID,
                            interestGroupID);
                    }
                } catch (IllegalArgumentException e) {
                    log.error("doResign exception: " + e.getMessage());
                }
            } else {
                log.error("doResignRequest: Can't find core name in join message");
            }
        } else {
            log.error("doResignRequest: Can't find uuid in join message");
        }
    }

    private void doUpdateJoin(String from, String packetId, String xml) {

        log.debug("doUpdateJoin: packetID=" + packetId + " from:" + from);

        String interestGroupID = null;
        Pattern pattern = Pattern.compile("uuid=[\"'](.+?)[\"']");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            interestGroupID = matcher.group(1);
        } else {
            log.error("doUpdateJoin: unable to extract  interestGroupID(uuid) from the received message.");
            return;
        }

        String productType = null;
        pattern = Pattern.compile("wpType=[\"'](.+?)[\"']");
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            productType = matcher.group(1);
        } else {
            log.error("doUpdateJoin: unable to extract  productType(wpType) from the received message.");
            return;
        }

        // notify the Interest Group Management Component of the deleted interest group
        interestGroupManager.updateJoinInterestGroup(interestGroupID, productType);
    }

    private void handleError(Packet packet) {

        ArbitraryIQ iq = (ArbitraryIQ) packet;
        XMPPError error = iq.getError();
        String from = iq.getFrom().replaceAll("/CoreConnection", "");
        log.error("Received error from: [" + from + "], Error: [" + iq.toXML() + "]");
        String coreJID = interestGroupManager.getCoreConnection().getJID();
        String coreName = interestGroupManager.getCoreConnection().getCoreNameFromJID(coreJID);
        String interestGroupID = "";
        String xml = iq.getChildElementXML();
        Matcher m = uuidPattern.matcher(xml);
        if (m.find()) {
            interestGroupID = m.group(1);
        } else {
            log.error("processPacket: Can't find uuid in join message");
        }

        String joinedKey = interestGroupID;

        // If the interest group is owned by this core then mark the other core in an ERROR
        // state and remove from joiningCores list
        if (interestGroupManager.isInterestGroupOwned(interestGroupID)) {
            interestGroupManager.removeJoiningCoreFromInterestGroup(interestGroupID, coreName);
            InterestGroup interestGroup = interestGroupManager.getOwnedInterestGroup(interestGroupID);
            if (interestGroup != null) {
                interestGroup.interestGroupOwner = coreName;
                interestGroup.state = CORE_STATUS.ERROR;
                log.error("ERROR from IncdMgmtIQListener interest group is owned by this core " +
                          iq.toXML());
            } else {
                log.error("ERROR from IncdMgmtIQListener target interest group was lost " +
                          iq.toXML());
            }
        }

        else if (interestGroupManager.isInterestGroupJoined(joinedKey)) {

            // If it is a CANCEL
            if (error.getType() == XMPPError.Type.CANCEL) {
                // and a service unavailable for a resign-request then just let this
                // interest group get resigned/deleted.
                if (error.getCode() == 503 || error.getCode() == 404) {
                    m = resignRequestPattern.matcher(xml);
                    if (m.find()) {
                        doResign(from, packet.getPacketID(), xml);
                    }
                }
            }
            // TODO: here is the place to handle an error like the following to
            // remove an interest group from a joined core when the owning core is not
            // responding.
            // 2008-02-25 16:14:24,437 ERROR [STDERR] ERROR from incdmgmt
            // <iq id="AUV53-50" to="interestmanager@danzig/manager"
            // from="interestmanager@clash/manager" type="error">
            // <incdmgmt xmlns="http://eoc.dctd.saic.com/incdmgmt">
            // <resign-request uuid="I-d25fd277-5820-4802-889e-176c05a8e266"
            // core="danzig"/>
            // </incdmgmt>
            // <error code="503" type="CANCEL"><service-unavailable
            // xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>
            // </error>
            // </iq>
        }

    }

    /*
     * This particular processPacket handles the interestGroup management IQ packets. (non-Javadoc)
     *
     * @see
     * org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware.smack.packet.Packet)
     */
    @Override
    public void processPacket(Packet packet) {

        log.debug("processPacket " + packet.toXML());
        if (packet instanceof ArbitraryIQ) {

            ArbitraryIQ iq = (ArbitraryIQ) packet;
            // log.debug("Got a incdmgmt IQ: "+packet.toXML());

            // strip off the /CoreConnection as JID
            String from = iq.getFrom().replaceAll("/CoreConnection", "");
            String packetId = iq.getPacketID();

            // Get initial interest group information
            String xml = iq.getChildElementXML();

            // Handle ERROR
            XMPPError error = iq.getError();
            if (error != null) {
                handleError(packet);
                return;
            }
            // Handle join
            Matcher m = joinPattern.matcher(xml);
            if (m.find()) {
                if (iq.getType() == IQ.Type.SET) {
                    doJoin(from, packetId, xml);
                } else if (iq.getType() == IQ.Type.RESULT) {
                    // If status is already-joined then doJoinReady
                    m = statusElementPattern.matcher(xml);
                    if (m.find()) {
                        String status = m.group(1);
                        m = alreadyJoinedPattern.matcher(status);
                        if (m.find()) {
                            doJoinReady(from, xml);
                        }
                    } else {
                        doJoinAcknowledge(from, packetId, xml);
                    }
                }
                return;
            }

            m = cleanupJoinedInterestGroupPattern.matcher(xml);
            if (m.find()) {
                doCleanupJoinedInterestGroup(from, packetId, xml);
            }

            m = deleteJoinedInterestGroupPattern.matcher(xml);
            if (m.find()) {
                doDeleteJoinedInterestGroup(from, packetId, xml);
            }

            m = deleteJoinedProductPattern.matcher(xml);
            if (m.find()) {
                doDeleteJoinedProduct(from, packetId, xml);
            }

            m = updateJoinPattern.matcher(xml);
            if (m.find()) {
                doUpdateJoin(from, packetId, xml);
            }

            // Handle resign
            m = resignPattern.matcher(xml);
            if (m.find()) {
                doResign(from, packetId, xml);
            }

            m = resignRequestPattern.matcher(xml);
            if (m.find()) {
                doResignRequest(from, packetId, xml);
            }

            m = resignedPattern.matcher(xml);
            if (m.find()) {
                doResignAcknowledge(from, packetId, xml);
            }

            // Handle ready
            m = readyPattern.matcher(xml);
            if (m.find()) {
                doJoinReady(from, xml);
                return;
            }

            // Handle Joined Publish Product Request
            m = joinedPublishPattern.matcher(xml);
            if (m.find()) {
                doJoinedPublishRequest(from, packetId, xml);
                return;
            }

            // Handle Product Publication Status Message
            m = productPublicationPattern.matcher(xml);
            if (m.find()) {
                doNotifyProductPublicationStatus(from, packetId, xml);
                return;
            }
        }
    }
}
