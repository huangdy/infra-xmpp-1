/*
 * PubSubPacketProvider.java
 * 
 * Created on Jun 20, 2007, 10:38:06 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub;

import java.io.IOException;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.PubSubEventExtension;

/**
 *
 * @author summersw
 */
public class PubSubEventProvider implements PacketExtensionProvider {

    public PubSubEventProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser xpp) {

        PubSubEventExtension pubsubEvent = new PubSubEventExtension("event",
                "http://jabber.org/protocol/pubsub#event");

        // System.out.println("PubSubEventProvider creating PubSubEventExtension");

        StringBuffer sb = new StringBuffer();
        StringBuffer itemBuffer = new StringBuffer();
        String rootElement = null;
        String rootNS = null;

        boolean done = false;
        boolean firstpass = true;
        boolean inItem = false;

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
                        // System.out.println("PubSubEventProvider root: "+rootElement);
                    }

                    if (inItem) {
                        if (!xpp.isEmptyElementTag()) {
                            // System.out.println("PubSubEventProvider start tag empty item: "+xpp.getText());
                            itemBuffer.append(xpp.getText());
                        }
                    }
                    if ("item".equals(xpp.getName())) {
                        // System.out.println("PubSubEventProvider starting add item: "+xpp.getText());
                        inItem = true;
                    }
                    // retract item from a node
                    if ("retract".equals(xpp.getName())) {
                        // System.out.println("PubSubEventProvider retract item: "+xpp.getText());
                        inItem = true;
                        if (xpp.isEmptyElementTag()) {
                            itemBuffer.append(xpp.getText());
                        }
                    }
                    // delete a node
                    if ("delete".equals(xpp.getName())) {
                        // System.out.println("PubSubEventProvider delete: "+xpp.getText());
                        inItem = true;
                        if (xpp.isEmptyElementTag()) {
                            itemBuffer.append(xpp.getText());
                        }
                    }

                    if (!xpp.isEmptyElementTag())
                        sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.END_TAG) {

                    // determine if we're done
                    // only end if hitting root element in the correct namespace
                    if (xpp.getName().equals(rootElement) && xpp.getNamespace().equals(rootNS)) {
                        done = true;
                    }

                    if ("item".equals(xpp.getName())) {
                        inItem = false;

                        // Add a new item
                        // System.out.println("PubSubEventProvider starting add item: "+itemBuffer.toString());
                        pubsubEvent.addItem(itemBuffer.toString());
                        itemBuffer.setLength(0);
                    }
                    if ("retract".equals(xpp.getName())) {
                        inItem = false;

                        // Add a new item
                        pubsubEvent.addRetract(itemBuffer.toString());
                        itemBuffer.setLength(0);
                    }
                    if ("delete".equals(xpp.getName())) {
                        inItem = false;

                        // Add a new item
                        pubsubEvent.addDelete(itemBuffer.toString());
                        itemBuffer.setLength(0);
                    }
                    if (inItem) {
                        itemBuffer.append(xpp.getText());
                    }

                    sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.TEXT) {

                    sb.append(StringUtils.escapeForXML(xpp.getText()));
                    if (inItem) {
                        // System.out.println("PubSubEventProvider adding text to item: "+xpp.getText());
                        itemBuffer.append(StringUtils.escapeForXML(xpp.getText()));
                    }
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

        pubsubEvent.setXML(sb.toString());

        return pubsubEvent;

    }
}
