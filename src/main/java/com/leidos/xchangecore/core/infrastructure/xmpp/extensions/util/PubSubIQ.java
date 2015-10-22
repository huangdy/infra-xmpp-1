package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PubSubIQ extends ArbitraryIQ {

    public static class Subscription {
        public String subscription;
        public String subid;
        public String jid;

        public Subscription(String subscription, String subid, String jid) {

            this.subscription = subscription;
            this.subid = subid;
            this.jid = jid;
        }
    }

    private Map<String, Subscription> subscriptions = new HashMap<String, Subscription>();
    private List<Item> items = new ArrayList<Item>();
    private String xml = "<null />";

    public PubSubIQ() {

        super();
    }

    public PubSubIQ(int id) {

        super(id);
    }

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

    public boolean hasItems() {

        return !items.isEmpty();
    }

    public Map<String, Subscription> getSubscriptions() {

        synchronized (subscriptions) {
            return Collections.unmodifiableMap(subscriptions);
        }
    }

    public void addSubscription(String jid, Subscription subscription) {

        synchronized (subscriptions) {
            subscriptions.put(jid, subscription);
        }
    }

    public boolean hasSubscriptions() {

        return !subscriptions.isEmpty();
    }
}
