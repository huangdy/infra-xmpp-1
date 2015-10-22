package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt;

import org.jivesoftware.smack.packet.Message;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.ArbitraryPacketExtension;

/**
 * This class provides a static factory for creating custom Message packets for the EOC-I project
 * interest management.
 * 
 * 
 * @see org.jivesoftware.smack.packet.Message
 * @see org.jivesoftware.smack.XMPPConnection
 */

public class InterestGrpManagementEventFactory {

    public static final String ELEMENT_NAME = "interestgroupmgmt";
    public static final String NAMESPACE = "http://uicds.saic.com/xmpp/extensions/interestgroupmgmt#event";

    /**
     * Joining a Core to an Interest Group
     * 
     * @param coreName
     *            name of the core to add to the interest group
     * @param uuid
     *            identifier for the interest group
     * @param name
     *            human-readable name of the interest group
     * @param owningJID
     *            JID of the core that owns the interest group
     * @return
     */
    public static Message sendUpdateToOwner(String coreName, String uuid, String from,
            String topic, String contents) {

        StringBuffer sb = new StringBuffer();
        ArbitraryPacketExtension ext = new ArbitraryPacketExtension(ELEMENT_NAME, NAMESPACE);
        Message msg = new Message();
        msg.setTo("InterestManager@" + coreName + "/manager");
        msg.setFrom(from);

        sb.append("<" + ELEMENT_NAME + " xmlns='" + NAMESPACE + "'>");
        sb.append("<uuid>" + uuid + "</uuid>");
        sb.append("<topic>" + topic + "</topic>");
        sb.append("<interestgroupmgmt_content>");
        sb.append(contents);
        sb.append("</interestgroupmgmt_content>");
        sb.append("</" + ELEMENT_NAME + ">");

        ext.setXML(sb.toString());
        msg.addExtension(ext);

        return msg;
    }
}
