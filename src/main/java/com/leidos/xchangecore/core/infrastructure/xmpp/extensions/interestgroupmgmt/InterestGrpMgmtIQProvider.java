/* InterestGrpMgmtIQProvider.java
 * 
 * Created on Aug 22, 2007, 10:38:06 PM
 * 
 */
package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.interestgroupmgmt;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.InterestGrpMgmtIQExtension;

/**
 * 
 * @author summersw
 */
public class InterestGrpMgmtIQProvider implements IQProvider {

    private Logger logger = Logger.getLogger(this.getClass());

    public InterestGrpMgmtIQProvider() {
        logger.debug("====#> InterestGrpMgmtIQProvider - constructor");
    }

    public IQ parseIQ(XmlPullParser xpp) throws Exception {

        InterestGrpMgmtIQExtension interestgroupmgmtIQ = new InterestGrpMgmtIQExtension();

        StringBuffer sb = new StringBuffer();
        StringBuffer itemBuffer = new StringBuffer();
        String rootElement = null;
        String rootNS = null;

        boolean done = false;
        boolean firstpass = true;
        boolean inItem = false;

        try {
            int eventType = xpp.getEventType();

            // logger.info("ParseIQ: eventType=" + eventType);

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
                            // System.out.println("interestgroupmgmtIQProvider start tag empty item: "+xpp.
                            // getText());
                            itemBuffer.append(xpp.getText());
                        }
                    }
                    if ("item".equals(xpp.getName())) {
                        // System.out.println("interestgroupmgmtIQProvider starting add item: "+xpp.getText())
                        // ;
                        inItem = true;
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
                        // System.out.println("InterestGrpMgmtIQProvider starting add item: "+itemBuffer.
                        // toString());
                        interestgroupmgmtIQ.addItem(itemBuffer.toString());
                        itemBuffer.setLength(0);
                    }

                    if (inItem) {
                        itemBuffer.append(xpp.getText());
                    }

                    sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.TEXT) {

                    sb.append(StringUtils.escapeForXML(xpp.getText()));
                    if (inItem) {
                        // System.out.println("InterestGrpMgmtIQProvider adding text to item: "+xpp.getText(
                        // ));
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

        interestgroupmgmtIQ.setChildElementXML(sb.toString());

        return interestgroupmgmtIQ;

    }
}
