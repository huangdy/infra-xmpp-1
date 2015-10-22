package com.leidos.xchangecore.core.infrastructure.xmpp.communications.ping;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class PingProvider implements IQProvider {

    // private final Logger logger = LoggerFactory.getLogger(PingProvider.class);

    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {

        // logger.debug("parseIQ: ");
        return new Ping();
    }
}
