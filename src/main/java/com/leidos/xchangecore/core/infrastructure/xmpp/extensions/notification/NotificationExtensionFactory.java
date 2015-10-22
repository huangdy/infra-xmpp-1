package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.notification;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.packet.XHTMLExtension;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.ArbitraryPacketExtension;

public class NotificationExtensionFactory {

    public static final String ELEMENT_NAME = "notification";
    public static final String NAMESPACE = "http://uicds.saic.com/xmpp/extensions/notification#event";

    public static Message createNotificationMessage(String to, String body, String xhtml,
            String notification) {

        StringBuffer sb = new StringBuffer();
        ArbitraryPacketExtension ext = new ArbitraryPacketExtension(ELEMENT_NAME, NAMESPACE);
        Message msg = new Message();
        msg.setTo(to);
        msg.setBody(body);

        if (xhtml != null) {
            XHTMLExtension xext = new XHTMLExtension();
            xext.addBody(xhtml);
            msg.addExtension(xext);
        }

        sb.append("<" + ELEMENT_NAME + " xmlns='" + NAMESPACE + "'>");
        sb.append(notification);
        sb.append("</" + ELEMENT_NAME + ">");

        ext.setXML(sb.toString());
        msg.addExtension(ext);

        return msg;
    }

}
