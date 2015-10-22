package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;

public interface CoreConnection
    extends XmppConnection {

    public abstract void checkRoster();

    public void cleanup();

    /**
     * Internal method to configure the connection with a properties file.
     * 
     * @param propsFile
     *            properties file to use for configuration values
     */
    public abstract void configure();

    public abstract ConnectionConfiguration getConfiguration();

    public abstract String getCoreNameFromJID(String coreJID);

    public abstract String getDebug();

    public abstract boolean getDebugBoolean();

    public abstract InterestGroupFileManager getFileManager();

    public abstract String getInterestGroupRoot();

    public abstract String getJIDFromCoreName(String coreName);

    public abstract String getJIDPlusResource();

    public abstract String getJIDPlusResourceFromCoreName(String coreName);

    public abstract String getPassword();

    public abstract String getPort();

    public abstract int getPortInt();

    public abstract String getResource();

    public abstract Map<String, String> getRosterByName();

    public abstract Map<String, String> getRosterStatus();

    public abstract String getServer();

    public abstract String getServername();

    public abstract String getUsername();

    public abstract Connection getXmppConnection();

    public void initialize();

    public abstract boolean isCoreInRoster(String coreName);

    public abstract boolean isCoreOnline(String coreName);

    public void ping(String remoteJID, boolean isConnected);

    public abstract void resetRemoteStatus(String remoteJID, boolean isOnline);

    public void sendCoreStatusUpdate(String remotJID,
                                     String coreStatus,
                                     String latitude,
                                     String longitude);

    public abstract void sendHeartBeat();

    public abstract void sendPacketCheckWellFormed(Packet packet) throws XMPPException;

    public abstract void setDebug(String value);

    public abstract void setInterestGroupRoot(String interestGroupRoot);

    public abstract void setRemoteCoreMutuallyAgreed(String remoteJID, boolean isMutuallyAgreed);

    public abstract void setPassword(String value);

    public abstract void setPort(String value);

    public abstract void setResource(String value);

    public abstract void setServer(String value);

    public abstract void setServername(String value);

    public abstract void setUsername(String value);

}