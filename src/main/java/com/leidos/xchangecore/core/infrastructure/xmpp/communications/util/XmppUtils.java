package com.leidos.xchangecore.core.infrastructure.xmpp.communications.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Various utility methods for manipulating XMPP strings, etc.
 * 
 * @author roger
 */
public class XmppUtils {

    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    /**
     * returns the pubsub service based on an input JID
     * 
     * @param owningCore
     * @return
     */
    public static String getPubsubServiceFromJID(String pubsubServicePrefix, String owningCore) {

        String server = org.jivesoftware.smack.util.StringUtils.parseServer(owningCore);
        return pubsubServicePrefix + "." + server;
    }

    /**
     * Checks if the xml is well-formed by parsing into a DocumentBuilder.
     * 
     * @param xml
     *            XML string to check for well-formedness
     * @return
     */
    public static boolean isWellFormed(String xml) {

        boolean isWellFormed = false;
        dbf.setValidating(false);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            try {
                InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                db.parse(is);
                isWellFormed = true;
            } catch (SAXException e) {
                isWellFormed = false;
            } catch (IOException e) {
                isWellFormed = false;
            }
        } catch (ParserConfigurationException e) {
            isWellFormed = false;
        }
        return isWellFormed;
    }

}
