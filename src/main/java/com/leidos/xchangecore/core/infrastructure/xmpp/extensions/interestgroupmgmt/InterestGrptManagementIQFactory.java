package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt;

import java.util.Map;

import org.jivesoftware.smack.packet.IQ;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.ArbitraryIQ;

/**
 * This class provides a static factory for creating custom IQ packets for the EOC-I project
 * interest management.
 * 
 * 
 * @see org.jivesoftware.smack.packet.IQ
 * @see org.jivesoftware.smack.XMPPConnection
 */

public class InterestGrptManagementIQFactory {

    public static final String elementName = "interestgroupmgmt";
    public static final String namespace = "http://uicds.saic.com/xmpp/extensions/interestgroupmgmt";

    /**
     * Joining core should respond with this to acknowledge it received the join request
     * 
     * @param to
     *            InterestManager and joining core
     * @param id
     *            packet id of join request IQ
     * @return IQ message to send
     */
    public static IQ acknowledgeJoinMessage(String to, String id) {

        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.RESULT);
        iq.setTo(to);
        iq.setPacketID(id);
        iq.setChildElementXML("<join/>");
        return iq;
    }

    /**
     * Result IQ if core requested to join is already joined`
     * 
     * @param to
     *            InterestManager and joining core
     * @param id
     *            packet id of join request IQ
     * @param originalJoin
     *            contents of original join message
     * @return IQ message to send
     */
    public static IQ alreadyJoinedMessage(String to, String id, String originalJoin) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.RESULT);
        iq.setTo(to);
        iq.setPacketID(id);

        // Add in a status message
        sb.append(originalJoin.replace("</interestgroupmgmt>",
                "<status>already-joined</status></interestgroupmgmt>"));

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Result IQ if core requested to join is the owner
     * 
     * @param to
     *            InterestManager and joining core
     * @param id
     *            packet id of join request IQ
     * @param originalJoin
     *            contents of original join message
     * @return IQ message to send
     */
    public static IQ alreadyOwnedError(String to, String id, String originalJoin) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.ERROR);
        iq.setTo(to);
        iq.setPacketID(id);

        sb.append(originalJoin);

        sb.append("<error type='cancel'>");
        sb.append("<unexpected-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>");
        sb.append("<invalid-owner xmlns=' http://uicds.saic.com/xmpp/extensions/interestgroupmgmt/join#errors'/>");
        sb.append("</error>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Result IQ if core has error when asked to join an interest group
     * 
     * @param to
     *            InterestManager and joining core
     * @param id
     *            packet id of join request IQ
     * @param originalJoin
     *            contents of original join message
     * @return IQ message to send
     */
    public static IQ coreError(String to, String id, String originalJoin) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.ERROR);
        iq.setTo(to);
        iq.setPacketID(id);

        sb.append(originalJoin);

        sb.append("<error type='cancel'>");
        sb.append("<error xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>");
        sb.append("<core-error xmlns=' http://uicds.saic.com/xmpp/extensions/interestgroupmgmt/join#errors'/>");
        sb.append("</error>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    public static IQ createCleanupJoinedInterestGroupMessage(String remoteJID, String IGID) {

        // System.out.println("createDeleteJoinedProductMessage: coreJID=" + coreJID + " productID="
        // + productID);

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(remoteJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<cleanupJoinedInterestGroup");
        sb.append(" uuid='" + IGID + "'");
        sb.append("/>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;

    }

    /**
     * send message indicating the deletion of a joined interest group . This message is sent by the
     * owning core to a joined core.
     * 
     * @param coreJID
     *            - JID of the core that where the publication request originated
     * @param intereset
     *            group ID - ID of the joined interest group
     * @return IQ message to send
     */
    public static IQ createDeleteJoinedInterestGroupMessage(String requestingCoreJID,
            String interestGroupID) {

        // System.out.println("createDeleteJoinedProductMessage: coreJID=" + coreJID + " productID="
        // + productID);

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(requestingCoreJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<deleteJoinedInterestGroup");
        sb.append(" uuid='" + interestGroupID + "'");
        sb.append("/>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * send message indicating the deletion of a joined work product . This message is sent by the
     * owning core to a joined core.
     * 
     * @param coreJID
     *            - JID of the core that where the publication request originated
     * @param productID
     *            - ID of the joined workProduct
     * @return IQ message to send
     */
    public static IQ createDeleteJoinedProductMessage(String requestingCoreJID, String productID) {

        // System.out.println("createDeleteJoinedProductMessage: coreJID=" + coreJID + " productID="
        // + productID);

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(requestingCoreJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<deleteJoinedProduct");
        sb.append(" uuid='" + productID + "'");
        sb.append("/>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Request to update a work product associated with a joined interest group. This message is
     * sent by a joined core to the owning core.
     * 
     * @param owningCoreJID
     *            JID of the core that owns the work product
     * @param interestGroupId
     *            identifier for the interest group
     * @param productId
     *            identifier of the product to update
     * @param productType
     *            type of the product to update
     * @param coreJID
     *            JID of the core requesting the product update
     * @param config
     *            Map of connection configuration parameters
     * @return IQ message to send
     */
    public static IQ createJoinedPublishProductRequestMessage(String owningCoreJID, String params,
            Map<String, String> config, String workProductEntry) {

        // System.out.println("createJoinMessage: coreJID=" + coreJID + " owningJID=" + owningJID);

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(owningCoreJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<requestJoinedPublish");
        sb.append(params);
        if (config.size() > 0) {
            sb.append(" <properties>");
            for (String key : config.keySet()) {
                sb.append("  <entry key='" + key + "'>" + config.get(key) + "</entry>");
            }
            sb.append(" </properties>");
        }
        sb.append(workProductEntry);
        sb.append("</requestJoinedPublish>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Joining a Core to an interest group
     * 
     * @param coreJID
     *            JID of the core to add to the interest group
     * @param uuid
     *            identifier for the interest group
     * @param name
     *            human-readable name of the interest group
     * @param owningJID
     *            JID of the core that owns the interest group
     * @param config
     *            Map of connection configuration parameters
     * @return IQ message to send
     */
    public static IQ createJoinMessage(String coreJID, String interestGroupInfoParams,
            Map<String, String> config, String interestGroupEntry) {

        // System.out.println("createJoinMessage: coreJID=" + coreJID + " owningJID=" + owningJID);

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(coreJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<join");
        sb.append(interestGroupInfoParams);
        sb.append(">");

        if (config.size() > 0) {
            sb.append(" <properties>");
            for (String key : config.keySet()) {
                sb.append("  <entry key='" + key + "'>" + config.get(key) + "</entry>");
            }
            sb.append(" </properties>");
        }
        sb.append(interestGroupEntry);
        sb.append("</join>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * send status of a pending product publication request to the requestor.
     * 
     * @param requestingCoreJID
     *            - JID of the core that where the publication request originated
     * @param params
     *            - extra parameter identifying the user ID and the core that processed the request
     * @param status
     *            - status of the request
     * @return IQ message to send
     */
    public static IQ createProductPublicationStatusMessage(String requestingCoreJID, String params,
            String statusEntry) {

        // System.out.println("createJoinMessage: coreJID=" + coreJID + " owningJID=" + owningJID);

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(requestingCoreJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<productPublicationStatus");
        sb.append(params);
        sb.append(statusEntry);
        sb.append("</productPublicationStatus>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * A joined core that has been requested to resign will return this message after it has fully
     * resigned itself from the interest group.
     * 
     * @param owningJID
     *            JID of the core that owns the interest group
     * @param uuid
     *            identifier for the interest group
     * @return IQ message to send
     */
    public static IQ createResignConfirmMessage(String to, String id, String originalJoin) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.RESULT);
        iq.setTo(to);
        iq.setPacketID(id);

        // Add in a status message
        sb.append(originalJoin.replace("<resign", "<resigned"));

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * The owner of an interest group will send this message to a joined core to request that it
     * resign from the interest group.
     * 
     * @param jid
     *            of the core to resign to the interest group
     * @param uuid
     *            identifier for the interest group
     * @param name
     *            human-readable name of the interest group
     * @param owningJID
     *            JID of the core that owns the interest group
     * @return IQ message to send
     */
    public static IQ createResignMessage(String coreJID, String uuid, String name, String owningJID) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(coreJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<resign");
        sb.append(" uuid='" + uuid + "'");
        sb.append(" name='" + name + "'");
        sb.append(" owner='" + owningJID + "'");
        sb.append("/>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * A joined core will send this message to the owning core to tell the owner that it is
     * resigning from the interest group.
     * 
     * @param coreJID
     *            joined core that is resigning
     * @param uuid
     *            identifier for the interest group
     * @param owningJID
     *            JID of the core that owns the interest group
     * @return IQ message to send
     */
    public static IQ createResigRequestMessage(String coreJID, String uuid, String owningJID) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(owningJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<resign-request");
        sb.append(" uuid='" + uuid + "'");
        sb.append(" core='" + coreJID + "'");
        sb.append("/>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * send message indicating a new work product type has been assoicated with a joined interest
     * group. This message is sent by the owning core.
     * 
     * @param coreJID
     *            - JID of the core that where the publication request originated
     * @param interestGroupID
     *            - ID of the joined interest group
     * @param productType
     *            - the associated workProduct Type
     * @return IQ message to send
     */
    public static IQ createUpdateJoinMessage(String requestingCoreJID, String interestGroupID,
            String productType) {

        // System.out.println("createUpdateJoinMessage: coreJID=" + coreJID + " interestGroupID="
        // + interestGroupID + " wpType=" + productType);

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(requestingCoreJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<updateJoin");
        sb.append(" uuid='" + interestGroupID + "'");
        sb.append(" wpType='" + productType + "'");
        sb.append("/>");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Joining core should send this when it has pulled the initial interest group state and is
     * ready to receive updates from the owning core.
     * 
     * @param uuid
     * @param owningJID
     * @return
     */
    public static IQ sendReadyMessage(String uuid, String owningJID) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(owningJID);

        sb.append("<" + elementName + " xmlns='" + namespace + "'>");

        sb.append("<ready");
        sb.append(" uuid='" + uuid + "'");
        sb.append(" />");

        sb.append("</" + elementName + ">");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

}
