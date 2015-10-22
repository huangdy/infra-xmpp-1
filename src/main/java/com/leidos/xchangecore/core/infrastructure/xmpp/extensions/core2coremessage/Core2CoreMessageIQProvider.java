package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.core2coremessage;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.Core2CoreMessageIQExtension;

public class Core2CoreMessageIQProvider implements IQProvider {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Core2CoreMessageIQProvider() {

        logger.debug("=====#> Core2CoreMessageIQProvider - constructor");
    }

    @Override
    public IQ parseIQ(XmlPullParser xpp) throws Exception {

        logger.debug("======#> parseIQ  called");

        Core2CoreMessageIQExtension core2CoreMessageIQ = new Core2CoreMessageIQExtension();

        StringBuffer sb = new StringBuffer();
        StringBuffer itemBuffer = new StringBuffer();
        String rootElement = null;
        String rootNS = null;

        boolean done = false;
        boolean firstpass = true;
        boolean inItem = false;

        try {
            int eventType = xpp.getEventType();

            logger.info("ParseIQ: eventType=" + eventType);

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
                            // System.out.println("core2CoreMessageIQProvider start tag empty item: "+xpp.
                            // getText());
                            itemBuffer.append(xpp.getText());
                        }
                    }
                    if ("item".equals(xpp.getName())) {
                        // System.out.println("core2CoreMessageIQProvider starting add item: "+xpp.getText())
                        // ;
                        inItem = true;
                    }

                    if (!xpp.isEmptyElementTag()) {
                        sb.append(xpp.getText());
                    }

                } else if (eventType == XmlPullParser.END_TAG) {

                    // determine if we're done
                    if (xpp.getName().equals(rootElement) && xpp.getNamespace().equals(rootNS)) {
                        done = true;
                    }

                    if ("item".equals(xpp.getName())) {
                        inItem = false;

                        // Add a new item
                        System.out.println("Core2CoreMessageIQProvider starting add item: "
                                + itemBuffer.toString());
                        core2CoreMessageIQ.addItem(itemBuffer.toString());
                        itemBuffer.setLength(0);
                    }

                    if (inItem) {
                        itemBuffer.append(xpp.getText());
                    }

                    sb.append(xpp.getText());

                } else if (eventType == XmlPullParser.TEXT) {

                    sb.append(StringUtils.escapeForXML(xpp.getText()));
                    if (inItem) {
                        System.out.println("Core2CoreMessageIQProvider adding text to item: "
                                + xpp.getText());
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

        core2CoreMessageIQ.setChildElementXML(sb.toString());

        return core2CoreMessageIQ;

    }
}
