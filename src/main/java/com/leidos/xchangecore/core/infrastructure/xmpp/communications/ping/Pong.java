package com.leidos.xchangecore.core.infrastructure.xmpp.communications.ping;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

public class Pong extends IQ {

    public Pong(Packet ping) {

        setType(IQ.Type.RESULT);
        setFrom(ping.getTo());
        setTo(ping.getFrom());
        setPacketID(ping.getPacketID());
    }

    @Override
    public String getChildElementXML() {

        return null;
    }

}
