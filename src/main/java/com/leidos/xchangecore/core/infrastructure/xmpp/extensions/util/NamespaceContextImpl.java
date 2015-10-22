/*
 * NamespaceContextImpl.java
 * 
 * Created on Jun 18, 2007, 11:51:47 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class NamespaceContextImpl implements NamespaceContext {

    private Map<String, String> map = new HashMap<String, String>();

    public NamespaceContextImpl() {
    }

    public void setNamespace(String prefix, String namespaceURI) {
        map.put(prefix, namespaceURI);
    }

    public String getNamespaceURI(String prefix) {
        return (String) map.get(prefix);
    }

    public String getPrefix(String namespaceURI) {

        String result = null;
        for (String prefix : map.keySet()) {
            String uri = (String) map.get(prefix);
            if (uri.equals(namespaceURI)) {
                result = prefix;
                break;
            }
        }
        return result;
    }

    public Iterator<?> getPrefixes(String namespaceURI) {
        List<String> prefixes = new ArrayList<String>();
        for (String prefix : map.keySet()) {
            String uri = (String) map.get(prefix);
            if (uri.equals(namespaceURI))
                prefixes.add(prefix);
        }
        return prefixes.iterator();
    }

}
