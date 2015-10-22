/**
 * 
 */
package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

public class Item {
    private String xml;

    public Item(String item) {
        xml = item;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String value) {
        xml = value;
    }

    public String toXML() {
        return xml;
    }
}
