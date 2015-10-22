package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class InterestGrpMgmtIQExtension extends ArbitraryIQ {

    private List<Item> items = new ArrayList<Item>();
    private String xml = "<null />";

    public String getChildElementXML() {

        return xml;

    }

    public void setChildElementXML(String xml) {

        this.xml = xml;

    }

    public Iterator<Item> getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(new ArrayList<Item>(items)).iterator();
        }
    }

    public void addItem(String xml) {
        synchronized (items) {
            items.add(new Item(xml));
        }
    }
}
