package com.leidos.xchangecore.core.infrastructure.xmpp.communications.ping;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidos.xchangecore.core.infrastructure.xmpp.communications.CoreConnection;

public class PingManager {

    // private final static String Logger logger = LoggerFactor
    private class PingPacketListener
    implements PacketListener {

        private final CoreConnection coreConnection;
        private final String localJID;

        public PingPacketListener(CoreConnection coreConnection) {

            this.coreConnection = coreConnection;
            // TODO We shall use environment variable instead of hard-coded xmpp username: xchangecore and/or uicds
            localJID = this.coreConnection.getJID().replaceAll("xchangecore@", "");
        }

        @Override
        public void processPacket(Packet packet) {

            // logger.debug("processPacket: received [" + packet.getFrom() + "] -> [" +
            // packet.getTo() + "]");
            if (packet.getFrom().contains(localJID)) {
                logger.warn("processPacket: received [" + packet.getFrom() + "] -> [" +
                    packet.getTo() + "]");
                return;
            }
            final Pong pong = new Pong(packet);
            // logger.debug("processPacket: send [" + pong.getFrom() + "] -> [" + pong.getTo() +
            // "]");
            coreConnection.sendPacket(pong);
        }
    }

    private class PingTasker
    implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(PingTasker.class);
        private final String remoteJID;
        private final int delay;
        private boolean isRunning = true;
        private boolean isConnected = false;

        PingTasker(String remoteJID, int delay) {

            logger.debug("create PinkTasker for " + remoteJID + " with interval: " + interval +
                " seconds");
            this.remoteJID = remoteJID;
            this.delay = delay;
        }

        @Override
        public void run() {

            try {
                // Sleep a minimum of 15 seconds plus delay before sending first heartbeat. This
                // will give time to
                // properly finish TLS negotiation and then start sending heartbeats.
                Thread.sleep(15000);
            } catch (final InterruptedException ie) {
                // Do nothing
            }

            while (isRunning) {
                if (coreConnection.getXmppConnection().isAuthenticated()) {
                    // logger.debug("ready to ping [" + remoteJID + "]");
                    isConnected = ping(remoteJID, isConnected);
                    // logger.debug("PingTasker: after ping: status: " + (isConnected ? "available"
                    // : "unavailable"));
                }
                try {
                    Thread.sleep(delay * 1000);
                } catch (final InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            logger.info("PingThread: " + Thread.currentThread().getName() + ": ... terminated ...");
        }

        public void setRunning(boolean isRunning) {

            this.isRunning = isRunning;
        }
    }

    public static final String NAMESPACE = "urn:xmpp:ping";

    public static final String ELEMENT = "ping";

    static {
        ProviderManager.getInstance().addIQProvider(ELEMENT, NAMESPACE, new PingProvider());
    }

    private final Logger logger = LoggerFactory.getLogger(PingManager.class);

    private final CoreConnection coreConnection;
    private final int interval;

    private final Map<String, PingTasker> taskerPool = new HashMap<String, PingTasker>();

    public PingManager(CoreConnection coreConnection, int interval) {

        this.coreConnection = coreConnection;
        final ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(coreConnection.getXmppConnection());
        sdm.addFeature(NAMESPACE);
        final PacketFilter pingPacketFilter = new PacketTypeFilter(Ping.class);
        coreConnection.getXmppConnection().addPacketListener(new PingPacketListener(this.coreConnection),
                                                             pingPacketFilter);
        this.interval = interval;
    }

    public void addRoster(String remoteJID) {

        final PingTasker pingTasker = new PingTasker(remoteJID, interval);
        final Thread pintThread = new Thread(pingTasker);
        pintThread.setDaemon(true);
        pintThread.setName("PingTasker: [" + remoteJID + "]");
        pintThread.start();

        taskerPool.put(remoteJID, pingTasker);
    }

    public boolean ping(String remoteJID, boolean isConnected) {

        boolean status;

        final Ping ping = new Ping(coreConnection.getJID(), remoteJID);
        // logger.debug("ping: [" + remoteJID + "]");

        final PacketCollector collector = coreConnection.createPacketCollector(new PacketIDFilter(ping.getPacketID()));
        coreConnection.sendPacket(ping);
        final IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        collector.cancel();

        status = (result == null) || (result.getType() == IQ.Type.ERROR) ? false : true;
        if ((result != null) && (result.getType() == IQ.Type.ERROR)) {
            // A Error response is as good as a pong response
            logger.debug("ping: " + remoteJID + ", error code: " + result.getError().getCode() +
                         ", condition: " + result.getError().getCondition());
        }

        // logger.debug("ping: " + remoteJID + " is " + (status == false ? "not" : "") +
        // " available ...");
        if (status != isConnected) {
            logger.debug("ping: status changed for [" + remoteJID + "]: previous status: " +
                (isConnected ? "available" : "unavailable") + ", new status: " +
                (status ? "available" : "unavailable"));
            coreConnection.resetRemoteStatus(remoteJID, status);
            logger.debug("ping: after resetRemoteStatus: [" + remoteJID +
                         (status ? "] available" : "] unavailable"));
        }

        return status;
    }

    public void removeRoster(String remoteJID) {

        final PingTasker t = taskerPool.remove(remoteJID);
        logger.debug("removeRoster: [" + remoteJID + "]");
        if (t != null) {
            t.setRunning(false);
        }
    }
}
