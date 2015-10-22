package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.ArbitraryIQ;

class IQNamespacePacketFilter
    implements PacketFilter {

    private Pattern nsPattern;
    private Logger log = Logger.getLogger(this.getClass());

    public IQNamespacePacketFilter(String namespace) {

        try {
            String p1 = "xmlns=[\"']" + namespace + "[\"']";
            nsPattern = Pattern.compile(p1);
        } catch (IllegalArgumentException e) {
            log.error("ERROR: IQNamespacePacketFilter Illegal argument to Pattern.compile");
            throw e;
        }
    }

    public boolean accept(Packet packet) {

        boolean ret = false;
        if (packet instanceof ArbitraryIQ) {
            ret = nsPattern.matcher(((ArbitraryIQ) packet).getChildElementXML()).find();
        }
        return ret;
    }
}