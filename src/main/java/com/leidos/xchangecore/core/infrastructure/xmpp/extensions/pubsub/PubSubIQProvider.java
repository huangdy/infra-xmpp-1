/*
 * PubSubIQProvider.java
 * 
 * Created on Jun 20, 2007, 10:57:14 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.PubSubIQ;

/**
 * 
 * @author summersw
 */
public class PubSubIQProvider implements IQProvider {

    public PubSubIQProvider() {

    }

    public IQ parseIQ(XmlPullParser xpp) throws Exception {

        // System.out.println("PubSubIQProvider:parseIQ");

        PubSubIQ pubsubIQ = new PubSubIQ();
        StringBuffer sb = new StringBuffer();
        StringBuffer itemBuffer = new StringBuffer();
        String rootElement = null;
        String rootNS = null;

        boolean done = false;
        boolean firstpass = true;
        boolean inItem = false;
        boolean inSub = false;

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
                    if (inItem) {
                        if (!xpp.isEmptyElementTag()) {
                            itemBuffer.append(xpp.getText());
                        }
                    }
                    if ("item".equals(xpp.getName())) {
                        // System.out.println("PubSubIQProvider adding item: "+xpp.getText());
                        inItem = true;
                    }

                    if ("subscription".equals(xpp.getName())) {
                        inSub = true;
                        String sub = xpp.getAttributeValue(null, "subscription");
                        String subid = xpp.getAttributeValue(null, "subid");
                        String jid = xpp.getAttributeValue(null, "jid");
                        if (jid != null) {
                            pubsubIQ.addSubscription(jid,
                                    new PubSubIQ.Subscription(sub, subid, jid));
                        }
                    }

                    if (!xpp.isEmptyElementTag())
                        sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.END_TAG) {

                    // determine if we're done
                    if (xpp.getName().equals(rootElement) && xpp.getNamespace().equals(rootNS)) {
                        done = true;
                    }
                    if ("item".equals(xpp.getName())) {
                        inItem = false;

                        // Add a new item
                        pubsubIQ.addItem(itemBuffer.toString());
                        itemBuffer.setLength(0);
                    }
                    if (inItem) {
                        itemBuffer.append(xpp.getText());
                    }

                    if ("subscription".equals(xpp.getName())) {
                        inSub = false;
                    }

                    sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.TEXT) {

                    sb.append(StringUtils.escapeForXML(xpp.getText()));
                    if (inItem) {
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

        pubsubIQ.setChildElementXML(sb.toString());

        return pubsubIQ;

    }
}
