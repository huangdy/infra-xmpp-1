package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;

import com.leidos.xchangecore.core.infrastructure.messages.AgreementRosterMessage;
import com.leidos.xchangecore.core.infrastructure.messages.Core2CoreMessage;
import com.leidos.xchangecore.core.infrastructure.messages.CoreRosterMessage;
import com.leidos.xchangecore.core.infrastructure.messages.DeleteInterestGroupMessage;
import com.leidos.xchangecore.core.infrastructure.messages.InterestGroupStateNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.messages.JoinedPublishProductRequestMessage;
import com.leidos.xchangecore.core.infrastructure.messages.NewInterestGroupCreatedMessage;
import com.leidos.xchangecore.core.infrastructure.messages.PingMessage;
import com.leidos.xchangecore.core.infrastructure.messages.ProductPublicationMessage;
import com.leidos.xchangecore.core.infrastructure.messages.ProductPublicationStatusMessage;
import com.leidos.xchangecore.core.infrastructure.messages.ShareInterestGroupMessage;
import com.leidos.xchangecore.core.infrastructure.messages.UnSubscribeMessage;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.util.XmppUtils;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.core2coremessage.Core2CoreMessageIQFactory;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.notification.NotificationExtensionFactory;

public class CommunicationsServiceXmppImpl {

    Logger logger = LoggerFactory.getLogger(CommunicationsServiceXmppImpl.class);

    private String defaultPropertiesFile;

    private MessageChannel coreRosterChannel;

    private InterestGroupManager interestGroupManager;

    // private MessageChannel getProductRequestChannel;
    //
    // public void setGetProductRequestChannel(MessageChannel channel) {
    // getProductRequestChannel = channel;
    // }

    private Core2CoreMessageProcessor core2CoreMessageProcessor;

    public void agreementRosterHandler(AgreementRosterMessage message) {

        logger.debug("agreementRosterHandler: roster changes for " + message.getCores().size() +
                     " cores");
        final Map<String, AgreementRosterMessage.State> cores = message.getCores();

        final Set<String> keys = cores.keySet();

        for (final String remoteJID : keys) {
            // log.debug("agreementRosterHandler: core:" + key + ", state:" + cores.get(key));
            final Pattern pattern = Pattern.compile("(.*)@(.*)");
            final Matcher matcher = pattern.matcher(remoteJID);
            if (matcher.matches()) {
                logger.debug("agreementRosterHandler: core: " + remoteJID + ", username: " +
                             matcher.group(1) + ", hostname: " + matcher.group(2));
                try {
                    switch (cores.get(remoteJID)) {
                    case CREATE:
                        logger.info("agreementRosterHandler: CREATE: " + remoteJID);
                        interestGroupManager.getCoreConnection().addRosterEntry(remoteJID,
                            remoteJID);
                        // interestGroupManager.getCoreConnection().addRosterEntry(key,
                        // matcher.group(2));
                        break;
                    case AMEND:
                        logger.info("agreementRosterHandler: AMEND: " + remoteJID +
                                    ": nothing been done");
                        break;
                    case RESCIND:
                        logger.info("agreementRosterHandler: RESCIND: remoteJID: " + remoteJID);
                        interestGroupManager.getCoreConnection().deleteRosterEntry(remoteJID);
                        sendRescindAgreementMessage(remoteJID);
                        logger.debug("agreementRosterHandler: request delete all interest group shared with " +
                                     remoteJID);
                        interestGroupManager.deleteInerestGroupAtJoinedCore(remoteJID);
                        logger.debug("agreementRosterHandler: remove " + remoteJID +
                                     " from joinCoreMap for all interest gorups");
                        interestGroupManager.unJoinedCore(remoteJID);
                        break;
                    }

                } catch (final Exception e) {
                    logger.info("Error: Caught exception while attempting to add roster entry for " +
                                matcher.group(2));
                    e.printStackTrace();
                }
            } else
                logger.error("******* Receiving invalid JID in agreementRosterHandler message - JID=" +
                             remoteJID + "   expected format[userName@hostName]");

        }
    }

    public void core2CoreMessageHandler(Core2CoreMessage message) {

        logger.debug("core2CoreMessageHandler: received message  for core " + message.getToCore());

        if (message.getMessageType() == null) {
            logger.error("received null message type to send: not sending");
            return;
        }

        if (message.getMessageType().equals("XMPP_MESSAGE")) {

            final org.jivesoftware.smack.packet.Message msg = NotificationExtensionFactory.createNotificationMessage(
                message.getToCore(), message.getBody(), message.getXhtml(), message.getMessage());
            // msg.setTo(message.getToCore());
            // msg.addBody(null, message.getMessage());
            try {
                interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
            } catch (final XMPPException e) {
                logger.error("Error sending XMPP message: " + e.getMessage());
            }
            return;
        }

        final String remoteCoreJID = interestGroupManager.getCoreConnection().getJIDFromCoreName(
            message.getToCore());
        final String remoteCoreJIDWithResource = interestGroupManager.getCoreConnection().getJIDPlusResourceFromCoreName(
            message.getToCore());

        final IQ msg = Core2CoreMessageIQFactory.createCore2CoreMessage(message.getMessage(),
            message.getMessageType(), remoteCoreJID, remoteCoreJIDWithResource);

        logger.debug(msg.toXML());

        try {
            interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("Error sending core 2 core message: " + e.getMessage());
        }
    }

    /**
     * Receives notification that a new interest group has been deleted.
     *
     * @param NewInterestGroupCreatedMessage
     */
    public void deleteInterestGroupHandler(DeleteInterestGroupMessage message) {

        logger.debug("deleteInterestGroupHandler - interestGroupID=" + message.getInterestGroupID());
        interestGroupManager.deleteInterestGroup(message.getInterestGroupID());
    }

    public Core2CoreMessageProcessor getCore2CoreMessageProcessor() {

        return core2CoreMessageProcessor;
    }

    public MessageChannel getCoreRosterChannel() {

        return coreRosterChannel;
    }

    public String getDefaultPropertiesFile() {

        return defaultPropertiesFile;
    }

    public InterestGroupManager getInterestGroupManager() {

        return interestGroupManager;
    }

    @PostConstruct
    public void initialize() {

        logger.info("CommunicationsServiceXmppImpl::init - started");
        // assert (getProductRequestChannel != null);
        assert interestGroupManager != null;
        assert core2CoreMessageProcessor != null;
        logger.info("CommunicationsServiceXmppImpl::init - completed");
    }

    public void joinedPublishProductRequestHandler(JoinedPublishProductRequestMessage message) {

        final String interestGroupID = message.getInterestGroupId();
        final String owningCore = message.getOwningCore();
        final String productId = message.getProductId();
        final String productType = message.getProductType();
        final String act = message.getAct();
        final String product = message.getWorkProduct();
        final String userID = message.getUserID();

        logger.debug("joinedPublishProductRequestHandler: act=" + act + "interestGroupID=" +
                     interestGroupID + " owningCore=" + owningCore + " productId=" + productId +
                     " productType=" + productType + " userID=" + userID);

        // send XEP message to the owning core's CommunicationXmppImpl
        interestGroupManager.requestJoinedPublishProduct(interestGroupID, owningCore, productId,
            productType, act, userID, product);

    }

    /**
     * Receives notification that a new interest group has been created.
     *
     * @param NewInterestGroupCreatedMessage
     */
    public void newInterestGroupCreatedHandler(NewInterestGroupCreatedMessage message) {

        logger.debug("newInterestGroupCreatedHandler: IGID: " + message.getInterestGroupID() +
                     " interestGroupType: " + message.getInterestGroupType());

        final InterestGroup interestGroup = new InterestGroup();
        interestGroup.interestGroupID = message.getInterestGroupID();
        interestGroup.interestGroupType = message.getInterestGroupType();
        interestGroup.interestGroupOwner = message.getOwningCore();
        interestGroup.workProductTypes = message.getJoinedWPTYpes();
        interestGroup.interestGroupPubsubService = XmppUtils.getPubsubServiceFromJID("pubsub",
            message.getOwningCore());

        if (!interestGroupManager.interestGroupExists(interestGroup.interestGroupID)) {
            logger.debug("newInterestGroupCreatedHandler: not found in InterestGroupManager.ownedInterestGroups");
            if (message.restored) {
                logger.debug("newInterestGroupCreatedHandler:  sharingStatus: " +
                             message.sharingStatus);
                if (message.sharingStatus.equals(InterestGroupStateNotificationMessage.SharingStatus.Joined.toString()))
                    interestGroupManager.restoreJoinedInterestGroup(interestGroup,
                        message.getOwnerProperties());
                else if (message.sharingStatus.equals(InterestGroupStateNotificationMessage.SharingStatus.Shared.toString()))
                    interestGroupManager.restoreSharedInterestGroup(interestGroup,
                        message.getSharedCoreList());
                else
                    interestGroupManager.restoreOwnedInterestGroup(interestGroup);
            } else
                interestGroupManager.createInterestGroup(interestGroup);
        }
    }

    public void pingHandler(PingMessage message) {

        logger.debug("pingHandler: ping: " + message.getRemoteJID() + ", previous status: " +
                     message.isConnected());
        interestGroupManager.getCoreConnection().ping(message.getRemoteJID(), message.isConnected());
    }

    /**
     * Receives message GetProductResponseMessage on the getProductResponseChannel Handler Spring
     * Integration message channel from the Communications Service.
     *
     * @param message
     *            Work product response message (GetProductResponseMessage)
     * @throws ApplicationException
     */
    public void productPublicationHandler(ProductPublicationMessage message) {

        final ProductPublicationMessage.PublicationType pubType = message.getPubType();
        final String interestGroupID = message.getInterestGroupID();
        final String wpID = message.getProductID();
        final String wpType = message.getProductType();
        final String wp = message.getProduct();

        logger.debug("productPublicationHandler: interestGroupID=" + interestGroupID + " wpID=" +
                     wpID + " wpType=" + wpType + " pubType=" + pubType);

        if (pubType.equals(ProductPublicationMessage.PublicationType.Publish))
            interestGroupManager.publishWorkProduct(interestGroupID, wpID, wpType, wp);
        else if (pubType.equals(ProductPublicationMessage.PublicationType.Delete))
            interestGroupManager.deleteWorkProduct(wpID, wpType, interestGroupID);
    }

    // This method is handled in the owning core
    // This method handles the status of a pending publication request. The status is transmitted
    // back to the requesting core via the XMPP connection
    public void productPublicationStatusHandler(ProductPublicationStatusMessage message) {

        final String userID = message.getUserID();
        final String requestingCore = message.getRequestingCore();
        final String status = message.getStatus();

        logger.debug("productPublicationStatusHandler: userID=" + userID + " requestingCore=" +
                     requestingCore + " status=[" + status + "]");

        interestGroupManager.sendProductPublicationStatus(requestingCore, userID, status);
    }

    // public String getWorkProduct(String interestGroupID, String wpID) {
    // return interestGroupManager.getWorkProduct(interestGroupID, wpID);
    // }

    public void sendCoreRoster() {

        Map<String, String> rosterStatusMap = new HashMap<String, String>();
        rosterStatusMap = interestGroupManager.getCoreConnection().getRosterStatus();

        // add ourself
        rosterStatusMap.put(interestGroupManager.getCoreConnection().getJID(), "available");

        final CoreRosterMessage msg = new CoreRosterMessage(rosterStatusMap);
        final Message<CoreRosterMessage> response = new GenericMessage<CoreRosterMessage>(msg);
        coreRosterChannel.send(response);
    }

    private void sendRescindAgreementMessage(String remoteJID) {

        final IQ msg = Core2CoreMessageIQFactory.createRescindAgreementMessage(remoteJID);

        logger.debug("sendRescindAgreementMessage: " + msg.toXML());

        try {
            interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
        } catch (final XMPPException e) {
            logger.error("sendRescindAgreementMessage: " + e.getMessage());
        }
    }

    public void setCore2CoreMessageProcessor(Core2CoreMessageProcessor core2CoreMessageProcessor) {

        this.core2CoreMessageProcessor = core2CoreMessageProcessor;
    }

    public void setCoreRosterChannel(MessageChannel coreRosterChannel) {

        this.coreRosterChannel = coreRosterChannel;
    }

    public void setDefaultPropertiesFile(String defaultPropertiesFile) {

        this.defaultPropertiesFile = defaultPropertiesFile;
    }

    public void setInterestGroupManager(InterestGroupManager interestGroupManager) {

        this.interestGroupManager = interestGroupManager;
    }

    public void shareInterestGroupHandler(ShareInterestGroupMessage message)
        throws IllegalArgumentException, IllegalStateException {

        interestGroupManager.shareInterestGroup(message);
    }

    public void systemInitializedHandler(String message) {

        logger.debug("systemInitializedHandler: ... start ...");
        sendCoreRoster();
        logger.debug("systemInitializedHandler: ... done ...");
    }

    public void unSubscribeHandler(UnSubscribeMessage message) {

        logger.debug("unSubscribeHandler: coreJID: " + message.getCoreJID() + ", IGID: " +
                     message.getInterestGroupID());

        final String pubSubSvc = "pubsub." +
            message.getCoreJID().substring(
                message.getCoreJID().indexOf("@") + 1);
        logger.debug("unSubscribeHandler: pubsubsvc: " + pubSubSvc + ", IGID: " +
                     message.getInterestGroupID());

        getInterestGroupManager().getInterestManager().unsubscribeAllForInterestGroup(pubSubSvc,
            message.getInterestGroupID());
    }
}