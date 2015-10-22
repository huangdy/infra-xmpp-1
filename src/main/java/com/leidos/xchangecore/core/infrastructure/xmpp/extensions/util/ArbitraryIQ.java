package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.xml.sax.SAXException;

public class ArbitraryIQ extends IQ {

    private String xml = "<null />";

    public static boolean usingIncrementalIDs = false;

    private static int id = 1;

    public ArbitraryIQ() {

        if (usingIncrementalIDs) {
            setPacketID(Integer.toString(id++));
        }
        // System.out.println("CREATED :"+(id-1));
    }

    public ArbitraryIQ(int id) {

        setPacketID(Integer.toString(id));
    }

    public static void resetIncrementalID() {

        // System.out.println("RESET");
        id = 1;
    }

    public String getChildElementXML() {

        return xml;

    }

    public void setChildElementXML(String xml) {

        this.xml = xml;

    }

    // TODO: When upgraded to smack 3.1.1 can replace this with
    // IQ:createResultError
    public static IQ createResultError(IQ iq, XMPPError error) {

        if (!(iq.getType() == Type.GET || iq.getType() == Type.SET)) {
            throw new IllegalArgumentException("IQ must be of type 'set' or 'get'. Original IQ: "
                    + iq.toXML());
        }
        IQ result = new IQ() {
            public String getChildElementXML() {

                return null;
            }
        };
        result.setType(Type.RESULT);
        result.setFrom(iq.getTo());
        result.setTo(iq.getFrom());
        result.setError(error);
        return result;
    }

    public boolean isWellFormed() {

        boolean isWellFormed = false;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            try {
                InputStream is = new ByteArrayInputStream(this.toXML().getBytes("UTF-8"));
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
