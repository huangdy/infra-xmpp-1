/*
 * PubSubPacketProvider.java
 * 
 * Created on Jun 20, 2007, 10:38:06 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt;

import java.io.IOException;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.InterestGrpMgmtEventExtension;

/**
 * 
 * @author summersw
 */
public class InterestGrpMgmtEventProvider implements PacketExtensionProvider {

    public InterestGrpMgmtEventProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser xpp) {

        InterestGrpMgmtEventExtension interestgroupmgmtEvent = new InterestGrpMgmtEventExtension(
                "interestgroupmgmt",
                "http://uicds.saic.com/xmpp/extensions/interestgroupmgmt#event");

        // System.out.println("interestgroupmgmtEventProvider creating interestgroupmgmtEventExtension");

        StringBuffer sb = new StringBuffer();
        StringBuffer content = new StringBuffer();
        String rootElement = null;
        String rootNS = null;

        boolean done = false;
        boolean firstpass = true;
        boolean inContent = false;
        boolean inUuid = false;
        boolean inTopic = false;

        try {
            int eventType = xpp.getEventType();

            while (!done) {

                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // we don't care since we're starting in the middle of a stanza

                } else if (eventType == XmlPullParser.END_DOCUMENT) {
                    // we don't care since we're starting in the middle of a stanza

                } else if (eventType == XmlPullParser.START_TAG) {

                    // capture the name of the root tag for this extension
                    if (firstpass) {
                        rootElement = xpp.getName();
                        rootNS = xpp.getNamespace();
                        firstpass = false;
                    }

                    if ("uuid".equals(xpp.getName())) {
                        inUuid = true;
                    }
                    if ("topic".equals(xpp.getName())) {
                        inTopic = true;
                    }
                    if (inContent) {
                        if (!xpp.isEmptyElementTag()) {
                            content.append(xpp.getText());
                        }
                    }
                    if ("interestgroupmgmt_content".equals(xpp.getName())) {
                        inContent = true;
                    }

                    if (!xpp.isEmptyElementTag())
                        sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.END_TAG) {

                    // determine if we're done
                    if (xpp.getName().equals(rootElement) && xpp.getNamespace().equals(rootNS)) {
                        done = true;
                    }

                    if ("interestgroupmgmt_content".equals(xpp.getName())) {
                        inContent = false;

                        // Add a new item
                        interestgroupmgmtEvent.setContent(content.toString());
                        content.setLength(0);
                    }
                    if (inContent) {
                        content.append(xpp.getText());
                    }
                    if ("uuid".equals(xpp.getName())) {
                        inUuid = false;
                    }
                    if ("topic".equals(xpp.getName())) {
                        inTopic = false;
                    }

                    sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.TEXT) {

                    if (inContent) {
                        content.append(StringUtils.escapeForXML(xpp.getText()));
                    }
                    if (inUuid) {
                        interestgroupmgmtEvent.setUuid(StringUtils.escapeForXML(xpp.getText()));
                    }
                    if (inTopic) {
                        interestgroupmgmtEvent.setTopic(StringUtils.escapeForXML(xpp.getText()));
                    }

                    sb.append(StringUtils.escapeForXML(xpp.getText()));
                }

                if (!done) {
                    eventType = xpp.next();
                }
            }

        } catch (XmlPullParserException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

        interestgroupmgmtEvent.setXML(sb.toString());

        return interestgroupmgmtEvent;

    }
}
