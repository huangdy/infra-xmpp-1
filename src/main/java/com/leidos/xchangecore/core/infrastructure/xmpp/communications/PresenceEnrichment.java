/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Packet;

/**
 *
 * @author vmuser
 */
public class PresenceEnrichment
    implements PacketInterceptor {

    private String latitude = "0.0";
    private String longitude = "0.0";

    PresenceEnrichment(String lat, String lon) {

        this.latitude = lat;
        this.longitude = lon;
    }

    @Override
    public void interceptPacket(Packet packet) {

        DefaultPacketExtension location = new DefaultPacketExtension("geoloc",
                                                                     "http://jabber.org/protocol/geoloc");
        location.setValue("lat", latitude);
        location.setValue("lon", longitude);
        packet.addExtension(location);

    }
}
