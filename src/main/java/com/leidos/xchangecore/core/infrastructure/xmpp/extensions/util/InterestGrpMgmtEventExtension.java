package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util;

import org.jivesoftware.smack.packet.PacketExtension;

public class InterestGrpMgmtEventExtension extends ArbitraryPacketExtension implements
        PacketExtension {

    private String uuid = null;
    private String topic = null;
    private String content = null;

    public InterestGrpMgmtEventExtension() {
    }

    public InterestGrpMgmtEventExtension(String elementName, String namespace) {
        super(elementName, namespace);
    }

    public InterestGrpMgmtEventExtension(String elementName, String namespace, String xml) {
        super(elementName, namespace, xml);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String toXML() {
        return super.toXML();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
