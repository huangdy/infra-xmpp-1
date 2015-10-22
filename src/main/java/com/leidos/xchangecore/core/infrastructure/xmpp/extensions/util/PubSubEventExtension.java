package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;

public class PubSubEventExtension extends ArbitraryPacketExtension implements PacketExtension {

    private List<Item> items = new ArrayList<Item>();
    private List<Item> retracts = new ArrayList<Item>();
    private List<Item> deletes = new ArrayList<Item>();

    public PubSubEventExtension() {
    }

    public PubSubEventExtension(String elementName, String namespace) {
        super(elementName, namespace);
    }

    public PubSubEventExtension(String elementName, String namespace, String xml) {
        super(elementName, namespace, xml);
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

    public Iterator<Item> getRetracts() {
        return Collections.unmodifiableList(new ArrayList<Item>(retracts)).iterator();
    }

    public void addRetract(String xml) {
        synchronized (retracts) {
            retracts.add(new Item(xml));
        }
    }

    public Iterator<Item> getDeletes() {
        return Collections.unmodifiableList(new ArrayList<Item>(deletes)).iterator();
    }

    public void addDelete(String xml) {
        synchronized (retracts) {
            deletes.add(new Item(xml));
        }
    }

    public String toXML() {
        return super.toXML();
    }

}
