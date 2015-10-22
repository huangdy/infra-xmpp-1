/*
 * ArbitraryPacketExtendion.java
 * 
 * Created on Jun 20, 2007, 2:47:45 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 *
 * @author summersw
 */
public class ArbitraryPacketExtension implements PacketExtension {

    private String elementName = null;
    private String namespace = null;
    private String xml = null;

    public ArbitraryPacketExtension() {
    }

    public ArbitraryPacketExtension(String elementName, String namespace) {
        this.elementName = elementName;
        this.namespace = namespace;
    }

    public ArbitraryPacketExtension(String elementName, String namespace, String xml) {
        this.elementName = elementName;
        this.namespace = namespace;
        this.xml = xml;
    }

    public void setXML(String xml) {
        this.xml = xml;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getElementName() {
        return this.elementName;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String toXML() {
        return xml;
    }

}
