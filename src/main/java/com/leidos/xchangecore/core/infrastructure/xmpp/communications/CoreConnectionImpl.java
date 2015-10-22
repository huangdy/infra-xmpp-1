package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;

import com.leidos.xchangecore.core.infrastructure.dao.AgreementDAO;
import com.leidos.xchangecore.core.infrastructure.messages.CoreStatusUpdateMessage;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.ping.PingManager;
import com.leidos.xchangecore.core.infrastructure.xmpp.communications.util.XmppUtils;

/**
 * This class provides an interface to an XMPP connection. This class is configured by a Spring
 * bean.
 *
 * <br />
 * <b>Todo:</b>
 * <ul>
 * <li>documenation - continue documenting</li>
 * </ul>
 *
 * <pre>
 * CoreConnection con = new CoreConnection();
 *
 * </pre>
 *
 * @see org.jivesoftware.smack.XMPPConnection
 *
 */

public class CoreConnectionImpl
    implements CoreConnection {

    public static final int BAD_FORMAT_CODE = 400;
    public static final String NOT_WELLFORMED_MSG = "Packet XML was not well-formed";
    public static final String BAD_FORMAT_CONDITION = "bad-format";

    protected XMPPConnection xmppConnection = null;
    protected Properties connectionProperties = null;

    // Properties
    private String debug = "false";
    private String server = null;
    private String servername = null;
    private String port = "5222";
    private String username = null;
    private String password = null;
    private String resource = "test";
    private String pubsubsvc = "pubsub";
    private String jid = null;
    private String jidPlusResource = null;
    private String interestGroupRoot = "/interestGroup";

    private int waitTimeInSeconds = 5;

    private int pingInterval;

    // private String coreConfigFile = null;
    private String coreLatLon;
    // privates
    private ConnectionConfiguration config = null;
    private ServiceDiscoveryManager discoManager;
    private RosterManager rosterManager;
    private PingManager pingManager;

    private final Logger logger = Logger.getLogger(this.getClass());

    // Manager to handle file transfers for the interest group
    private InterestGroupFileManager fileManager = null;

    // boolean connected = false;

    private MessageChannel coreStatusUpdateChannel;

    private AgreementDAO agreementDAO;

    public CoreConnectionImpl() {

    }

    public CoreConnectionImpl(Properties ownerConnectionProps, String coreName) {

        // This constructor is called to create a xmpp connection to a remote core

        // Set the properties
        connectionProperties = ownerConnectionProps;

        // Load the properties, configure, and connect
        loadProperties();

        // configure and connect
        configureOwnerConnection(coreName);
        connect();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#addPacketListener(org.jivesoftware
     * .smack.PacketListener, org.jivesoftware.smack.filter.PacketFilter)
     */
    @Override
    public void addPacketListener(PacketListener listener, PacketFilter packetFilter) {

        xmppConnection.addPacketListener(listener, packetFilter);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#addRosterEntry(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void addRosterEntry(String remoteJID, String name) {

        final ItemType subscription = rosterManager.createEntry(remoteJID, name, null);
        logger.debug("addRosterEntry: [" + remoteJID + "]: subscription: " + subscription);
        getAgreementDAO().setRemoteCoreMutuallyAgreed(remoteJID, subscription.equals(ItemType.both));
        logger.debug("addRosterEntry: after setRemoteCoreMutuallyAgreed: " + subscription);
        pingManager.addRoster(remoteJID);
        logger.debug("addRosterEntry: after PingManager.addRoster: " + remoteJID);
    }

    private void checkForWellFormedPacket(Packet packet) throws XMPPException {

        // Throw an exception if the packet XML is not well formed
        if (!XmppUtils.isWellFormed(packet.toXML())) {
            final XMPPError error = new XMPPError(BAD_FORMAT_CODE,
                XMPPError.Type.MODIFY,
                BAD_FORMAT_CONDITION,
                NOT_WELLFORMED_MSG,
                null);
            throw new XMPPException(error);
        }
    }

    @Override
    public void checkRoster() {

        // TODO - ddh rosterManager.checkRoster();
    }

    @Override
    public void cleanup() {

        // shall we unsubscribe the remoteJIDs ???
        rosterManager.unSubscribeAll();
        disconnect();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#configure()
     */
    @Override
    public void configure() {

        try {
            // Set the JID for this connection
            if (servername != null)
                jid = username + "@" + servername;
            else
                jid = username + "@" + server;

        } catch (final Exception e) {
            logger.error("An error occurred reading the properties file.");
            System.exit(0);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#configure()
     */
    public void configureOwnerConnection(String coreName) {

        configure();

        // JID core connection to remote core = ownerCoreJID + "/" + joiningCoreName
        // e.g. joiningCoreName = "Core2", owningCoreName = "Core1", user = "uicds"
        // ===> JID = "uocds@Core1/Core2"
        jid += "/" + coreName;
        setResource(coreName);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#connect()
     */
    @Override
    public void connect() {

        try {
            // connect and login to server
            logger.info("    server: " + getServer());
            logger.info("      port: " + getPort());
            logger.info("  username: " + getUsername());
            logger.info("  resource: " + getResource());

            Connection.DEBUG_ENABLED = getDebugBoolean();

            config = new ConnectionConfiguration(getServer(), getPortInt());

            // If xmppConnection is not created yet then create one. This may have been
            // configured via Spring or during testing setup.
            xmppConnection = new XMPPConnection(config);

            logger.info("===> login as " + getUsername() + " resource=" + getResource());
            xmppConnection.connect();
            xmppConnection.login(getUsername(), getPassword(), getResource());

            if (getCoreLatLon() != null && getCoreLatLon().length() > 0) {
                final String[] pos = getCoreLatLon().split("[ ,]");
                if (pos.length == 2) {
                    logger.debug("adding geo to presence: " + getCoreLatLon());
                    // add packet interceptor
                    final PacketFilter presenceFilter = new PacketTypeFilter(Presence.class);
                    final PresenceEnrichment presenceEnrichment = new PresenceEnrichment(pos[0],
                                                                                         pos[1]);
                    xmppConnection.addPacketInterceptor(presenceEnrichment, presenceFilter);
                }
            } else
                logger.debug("No lat/lon configured");

            // Set my resource priority
            setResourcePriority();

            // Create the roster manager
            logger.info("Instantiating RosterManger");
            rosterManager = new RosterManager(this);

            // Set the name of this connection to the roster name for this core
            if (rosterManager.getRosterNameFromJID(getJID()) != null) {
                logger.info("====> Set connection name to " +
                            rosterManager.getRosterNameFromJID(getJID()));
                logger.debug("Adding ourself: JID=" + getJID() + " name=" +
                             rosterManager.getRosterNameFromJID(getJID()) + " to roster");
                rosterManager.createEntry(getJID(), rosterManager.getRosterNameFromJID(getJID()),
                    null);
            }

            logger.info("initialize PingManger ...");
            pingManager = new PingManager(this, getPingInterval());

            // Two different attempts to get things to shutdown cleanly
            // neither seems to work correctly, each call to disconnect hangs
            xmppConnection.addConnectionListener(new ConnectionCleanup(getServer(), this));

            // Obtain the ServiceDiscoveryManager associated with my XMPPConnection
            discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

            fileManager = new InterestGroupFileManager(this);
        } catch (final XMPPException e) {
            e.printStackTrace();
            logger.error("CoreConnection XMPPException connecting: " + e);
            return;
        } catch (final Exception e) {
            e.printStackTrace();
            logger.error("CoreConnection Exception connecting: " + e.getMessage() + " " +
                         e.toString());
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.leidos.xchangecore.core.infrastructure.xmpp.communications.XmppConnection#
     * createCommandWithReply(org.jivesoftware.smack.packet.Packet)
     */
    @Override
    public CommandWithReply createCommandWithReply(Packet packet) throws XMPPException {

        checkForWellFormedPacket(packet);

        return new CommandWithReplyImpl(this, packet);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#createPacketCollector(org.jivesoftware
     * .smack.filter.PacketFilter)
     */
    @Override
    public PacketCollector createPacketCollector(PacketFilter packetFilter) {

        return xmppConnection.createPacketCollector(packetFilter);
    }

    //
    // /**
    // * Return the actual XMPP connection from the Smack library.
    // *
    // * @return connected instance of org.jivesoftware.smack.XMPPConnection
    // */
    // public XMPPConnection getConnection() {
    // return this.con;
    // }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#deleteRosterEntry(java.lang.String)
     */
    @Override
    public void deleteRosterEntry(String remoteJID) {

        rosterManager.removeEntry(remoteJID);
        pingManager.removeRoster(remoteJID);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#disconnect()
     */
    @Override
    public void disconnect() {

        logger.info("CoreConnection.disconnect");
        if (xmppConnection == null) {
            logger.error("null connection at disconnect");
            return;
        }
        if (xmppConnection.isConnected())
            // TODO: might add a disconnect listener interface to and have the InterestManagement
            // class register and then do this unsubscribeAll if we really need to do this
            // interestManager.unsubscribeAll();
            xmppConnection.disconnect();
        // ddh connected = xmppConnection.isConnected();
    }

    @Override
    public DiscoverInfo discoverNodeInfo(String node) {

        if (discoManager != null)
            try {
                logger.debug("discoverNodeInfo: pubsbusvc: " + pubsubsvc + ", node: " + node);
                return discoManager.discoverInfo(pubsubsvc, node);
            } catch (final XMPPException e) {
                final XMPPError err = e.getXMPPError();
                if (err != null && err.getCode() == 404) {
                    if (!isConnected())
                        logger.error("XMPP Server not found (not connected)");
                } else {
                    logger.error("discovering info for node " + node);
                    if (err != null) {
                        logger.error("  message: " + err.getMessage());
                        logger.error("     code: " + err.getCode());
                        logger.error("     type: " + err.getType());
                    } else
                        logger.error("  null XMPP error message");

                }
            }
        return null;
    }

    @Override
    public DiscoverItems discoverNodeItems(String node) {

        if (discoManager != null)
            try {
                return discoManager.discoverItems(pubsubsvc, node);
            } catch (final XMPPException e) {
                final XMPPError err = e.getXMPPError();
                if (err != null && err.getCode() == 404) {
                    if (!isConnected())
                        logger.error("XMPP Server not found (not connected)");
                    else
                        logger.error("XMPP Server not found error. pubsub." +
                                     xmppConnection.getHost() + " may not be resolvable");
                } else {
                    logger.error("discovering items for node: " + node);
                    if (err != null) {
                        logger.error("  message: " + err.getMessage());
                        logger.error("     code: " + err.getCode());
                        logger.error("     type: " + err.getType());
                    } else
                        logger.error(" null XMPP error message");

                }
            }
        return null;
    }

    public AgreementDAO getAgreementDAO() {

        return agreementDAO;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getConfiguration()
     */
    @Override
    public ConnectionConfiguration getConfiguration() {

        return config;
    }

    private String getCoreLatLon() {

        return coreLatLon;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#getCoreNameFromJID(java.lang.String)
     */
    @Override
    public String getCoreNameFromJID(String coreJID) {

        return rosterManager.getRosterNameFromJID(org.jivesoftware.smack.util.StringUtils.parseBareAddress(
            coreJID).toLowerCase());
    }

    public MessageChannel getCoreStatusUpdateChannel() {

        return coreStatusUpdateChannel;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getDebug()
     */
    @Override
    public String getDebug() {

        return debug;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getDebugBoolean()
     */
    @Override
    public boolean getDebugBoolean() {

        return new Boolean(debug).booleanValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getFileManager()
     */
    @Override
    public InterestGroupFileManager getFileManager() {

        return fileManager;
    }

    FileTransferManager getFileTransferManager() {

        return new FileTransferManager(xmppConnection);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getInterestGroupRoot()
     */
    @Override
    public String getInterestGroupRoot() {

        return interestGroupRoot;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJID()
     */
    @Override
    public String getJID() {

        // logger.info("CoreConnectionImpl.getJID: jid=" + jid);
        return jid;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJIDFromCoreName(java.lang.String)
     */
    @Override
    public String getJIDFromCoreName(String coreName) {

        return rosterManager.getJIDFromRosterName(coreName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJIDPlusResource()
     */
    @Override
    public String getJIDPlusResource() {

        return jidPlusResource;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJIDFromCoreName(java.lang.String)
     */
    @Override
    public String getJIDPlusResourceFromCoreName(String coreName) {

        return rosterManager.getJIDFromRosterName(coreName) + "/" + resource;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPassword()
     */
    @Override
    public String getPassword() {

        return password;
    }

    public int getPingInterval() {

        return pingInterval;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPort()
     */
    @Override
    public String getPort() {

        return port;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPortInt()
     */
    @Override
    public int getPortInt() {

        return new Integer(port).intValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPubSubSvc()
     */
    @Override
    public String getPubSubSvc() {

        return pubsubsvc;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getResource()
     */
    @Override
    public String getResource() {

        return resource;
    }

    Roster getRoster() {

        return xmppConnection.getRoster();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getRosterByName()
     */
    @Override
    public Map<String, String> getRosterByName() {

        return rosterManager.getRosterByName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getRosterStatus()
     */
    @Override
    public Map<String, String> getRosterStatus() {

        return rosterManager.getRosterStatus();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getServer()
     */
    @Override
    public String getServer() {

        return server;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getServername()
     */
    @Override
    public String getServername() {

        return servername;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getUsername()
     */
    @Override
    public String getUsername() {

        return username;
    }

    @Override
    public int getWaitTimeInSeconds() {

        return waitTimeInSeconds;
    }

    @Override
    public Connection getXmppConnection() {

        return xmppConnection;
    }

    /**
     * Constructor - use default and roster properties files, defined in the applicationContext, for
     * configuration parameters.
     */
    @PostConstruct
    @Override
    public void initialize() {

        logger.info("initialize: ... start ...");
        // Configure and connect;
        assert coreStatusUpdateChannel != null;
        configure();
        connect();
        logger.info("initialize: ... done ...");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#isConnected()
     */
    @Override
    public boolean isConnected() {

        return xmppConnection.isConnected();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#isCoreInRoster(java.lang.String)
     */
    @Override
    public boolean isCoreInRoster(String coreName) {

        return rosterManager.isCoreInRoster(coreName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#isCoreOnline(java.lang.String)
     */
    @Override
    public boolean isCoreOnline(String coreName) {

        if (coreName == null)
            return false;
        return rosterManager.isCoreOnline(coreName);
    }

    private void loadProperties() {

        // set the property members for the connection
        setDebug(connectionProperties.getProperty("debug"));
        setServer(connectionProperties.getProperty("server"));
        setServername(connectionProperties.getProperty("servername"));
        setPort(connectionProperties.getProperty("port"));
        setUsername(connectionProperties.getProperty("username"));
        setPassword(connectionProperties.getProperty("password"));
        setResource(connectionProperties.getProperty("resource"));
        setPubSubSvc(connectionProperties.getProperty("pubsubsvc"));
        setInterestGroupRoot(connectionProperties.getProperty("root", "/interestGroup"));
    }

    @Override
    public void ping(String remoteJID, boolean isConnected) {

        pingManager.ping(remoteJID, isConnected);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#removePacketListener(org.jivesoftware
     * .smack.PacketListener)
     */
    @Override
    public void removePacketListener(PacketListener listener) {

        xmppConnection.removePacketListener(listener);
    }

    private String replaceHostname(String value) {

        String localhost = "";
        try {
            localhost = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return value.replace("localhost", localhost);
    }

    @Override
    public void resetRemoteStatus(String remoteJID, boolean isOnline) {

        final RosterEntry roster = getRoster().getEntry(remoteJID);
        if (roster != null) {
            final ItemType subscription = roster.getType();
            if (isOnline) {
                if (subscription.equals(ItemType.both)) {
                    logger.debug("resetRemoteStatus: set the remoteJID: " + remoteJID + " to " +
                                 (isOnline ? "available" : "unavailable"));
                    sendCoreStatusUpdate(remoteJID, "available", "", "");
                }
            } else if (subscription.equals(ItemType.both))
                sendCoreStatusUpdate(remoteJID, "unavailable", "", "");
        }
        logger.debug("resetRemoteStatus: ... exit ...");
    }

    @Override
    public void sendCoreStatusUpdate(String remotJID,
                                     String coreStatus,
                                     String latitude,
                                     String longitude) {

        logger.info("sendCoreStatusUpdate: JID: " + remotJID + ", status: " + coreStatus +
                    (latitude.length() > 0 ? ", " + latitude + "/" + longitude : ""));

        final CoreStatusUpdateMessage msg = new CoreStatusUpdateMessage(remotJID,
            coreStatus,
            latitude,
            longitude);
        final Message<CoreStatusUpdateMessage> update = new GenericMessage<CoreStatusUpdateMessage>(msg);
        synchronized (this) {
            getCoreStatusUpdateChannel().send(update);
            logger.info("sendCoreStatusUpdate: ... exit ...");
        }
    }

    @Override
    public void sendHeartBeat() {

        if (isConnected())
            try {
                // send a heartbeat to remote cores by updating our presence
                final Presence presence = new Presence(Type.available, "Online", 50, Mode.available);
                sendPacket(presence);
            } catch (final Exception e) {
                if (e instanceof XMPPException) {
                    final XMPPException xe = (XMPPException) e;
                    logger.error("XMPPException sending heartbeat: " + xe.getMessage());
                    final XMPPError error = xe.getXMPPError();
                    logger.error("XMPP Error Message  : " + error.getMessage());
                    logger.error("XMPP Error Condition: " + error.getCondition());
                    logger.error("XMPP Error Type     : " + error.getType());
                    logger.error("XMPP Error Code     : " + error.getCode());

                } else
                    logger.error("Exception sending heartbeat: " + e.getMessage());
            }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#sendPacket(org.jivesoftware.smack.
     * packet.Packet)
     */
    @Override
    public void sendPacket(Packet packet) {

        xmppConnection.sendPacket(packet);
    }

    // public String getServerFromRosterName(String coreName) {
    // return rosterManager.getServerFromRosterName(coreName);
    // }
    //
    // public String getJIDFromRosterName(String coreName) {
    // return rosterManager.getJIDFromRosterName(coreName);
    // }
    //
    // public String getCoresRosterNameFromServerName(String coreServer) {
    // return rosterManager.getCoresRosterNameFromServerName(coreServer);
    // }

    /*
     * (non-Javadoc)
     *
     * @see com.leidos.xchangecore.core.infrastructure.xmpp.communications.CoreConnection#
     * sendPacketCheckWellFormed(org.jivesoftware.smack.packet.Packet)
     */
    @Override
    public void sendPacketCheckWellFormed(Packet packet) throws XMPPException {

        checkForWellFormedPacket(packet);

        sendPacket(packet);
    }

    public void setAgreementDAO(AgreementDAO agreementDAO) {

        this.agreementDAO = agreementDAO;
    }

    /*
     * public void sendJoinMessage(String coreName, InterestGroup interestGroup) {
     * interestManager.sendJoinMessage(rosterManager.getJIDFromRosterName(coreName), interestGroup);
     * }
     *
     * public void sendResignMessage(String coreName, InterestGroup interestGroup) {
     * interestManager.sendResignMessage(rosterManager.getJIDFromRosterName(coreName),
     * interestGroup); }
     *
     * public void transferFileToCore(String coreName, String fileName) {
     * fileManager.transferFileToCore(rosterManager.getJIDFromRosterName(coreName) + "/manager",
     * fileName); }
     *
     * public void sendResignRequestMessage(String coreName, InterestGroup interestGroup) {
     * interestManager.sendResignRequestMessage(rosterManager.getJIDFromRosterName(coreName),
     * interestGroup); }
     */

    public void setCoreLatLon(String coreLatLon) {

        this.coreLatLon = coreLatLon;
    }

    public void setCoreStatusUpdateChannel(MessageChannel coreStatusUpdateChannel) {

        this.coreStatusUpdateChannel = coreStatusUpdateChannel;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setDebug(java.lang.String)
     */
    @Override
    public void setDebug(String value) {

        if (value != null && !value.equals(null))
            debug = value;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#setInterestGroupGroupRoot(java.lang
     * .String)
     */
    @Override
    public void setInterestGroupRoot(String interestGroupRoot) {

        this.interestGroupRoot = interestGroupRoot;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setPassword(java.lang.String)
     */
    @Override
    public void setPassword(String value) {

        if (value != null && !value.equals(null))
            password = value;
    }

    /*
     * ddh public String getCoreConfigFile() { return coreConfigFile; }
     *
     * public void setCoreConfigFile(String coreConfigFile) { this.coreConfigFile = coreConfigFile;
     * }
     */

    public void setPingInterval(int pingInterval) {

        this.pingInterval = pingInterval;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setPort(java.lang.String)
     */
    @Override
    public void setPort(String value) {

        if (value != null && !value.equals(null))
            port = value;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setPubSubSvc(java.lang.String)
     */
    @Override
    public void setPubSubSvc(String value) {

        if (value != null && !value.equals(null))
            pubsubsvc = replaceHostname(value);

    }

    @Override
    public void setRemoteCoreMutuallyAgreed(String remoteJID, boolean isMutuallyAgreed) {

        getAgreementDAO().setRemoteCoreMutuallyAgreed(remoteJID, isMutuallyAgreed);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setResource(java.lang.String)
     */
    @Override
    public void setResource(String value) {

        if (value != null && !value.equals(null)) {
            resource = value;
            if (servername != null)
                jidPlusResource = username + "@" + servername;
            else
                jidPlusResource = username + "@" + server;
            if (resource != null && resource != "")
                jidPlusResource += "/" + resource;
        }
    }

    protected void setResourcePriority() {

        xmppConnection.sendPacket(new Presence(Presence.Type.available,
                                               null,
                                               77,
                                               Presence.Mode.available));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setServer(java.lang.String)
     */
    @Override
    public void setServer(String value) {

        if (value != null && !value.equals(null))
            server = replaceHostname(value);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setServername(java.lang.String)
     */
    @Override
    public void setServername(String value) {

        if (!value.equals(null))
            servername = replaceHostname(value);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setUsername(java.lang.String)
     */
    @Override
    public void setUsername(String value) {

        if (value != null && !value.equals(null))
            username = value;
    }

    public void setWaitTimeInSeconds(int waitTimeInSeconds) {

        this.waitTimeInSeconds = waitTimeInSeconds;
    }

}
