package com.leidos.xchangecore.core.infrastructure.xmpp.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.junit.Ignore;
import org.junit.Test;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory;
import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.ArbitraryIQ;

/**
 * @author roger
 *
 */
public class PubSubIQTest {

    final static String svc = "pubsub";
    final static String nodeName = "/node";
    final static String discoverNodeString = "<query xmlns=\"http://jabber.org/protocol/disco#info\" node=\"" +
                                             nodeName + "\"></query>";
    final static String discoverItemsString = "<query xmlns=\"http://jabber.org/protocol/disco#items\" node=\"" +
                                              nodeName + "\"></query>";

    private void checkPubSubGetIQ(IQ iq) {

        assertNotNull("iq is null", iq.toXML());
        assertEquals("wrong to", iq.getTo(), svc);
        assertEquals("wrong type", iq.getType(), Type.GET);
    }

    private void checkIQIsWellFormed(IQ iq) {

        if (iq instanceof ArbitraryIQ) {
            assertTrue("IQ is not well formed", ((ArbitraryIQ) iq).isWellFormed());
        }
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#discoverInfo(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testDiscoverInfo() {

        IQ iq = PubSubIQFactory.discoverInfo(svc, nodeName);
        checkPubSubGetIQ(iq);
        assertTrue(iq.getChildElementXML().equals(discoverNodeString));
        checkIQIsWellFormed(iq);
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#discoverItems(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testDiscoverItems() {

        IQ iq = PubSubIQFactory.discoverItems(svc, nodeName);
        checkPubSubGetIQ(iq);
        checkIQIsWellFormed(iq);
        assertTrue(iq.getChildElementXML().equals(discoverItemsString));
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#retrieveSubscriptions(java.lang.String)}.
     */
    @Test
    public void testRetrieveSubscriptions() {

        IQ iq = PubSubIQFactory.retrieveSubscriptions(svc);
        checkIQIsWellFormed(iq);
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#retrieveAffiliations(java.lang.String)}.
     */
    @Ignore
    @Test
    public void testRetrieveAffiliations() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#subscribeNode(java.lang.String, java.lang.String, java.lang.String, org.jivesoftware.smackx.packet.DataForm)}.
     */
    @Ignore
    @Test
    public void testSubscribeNode() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#unsubscribeNode(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Ignore
    @Test
    public void testUnsubscribeNode() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#configureSubscriptionOptions(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.jivesoftware.smackx.packet.DataForm)}.
     */
    @Ignore
    @Test
    public void testConfigureSubscriptionOptions() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#retrieveItems(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])}.
     */
    @Ignore
    @Test
    public void testRetrieveItems() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#retrieveItem(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Ignore
    @Test
    public void testRetrieveItem() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#publishItem(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Ignore
    @Test
    public void testPublishItem() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#deleteItems(java.lang.String, java.lang.String, java.lang.String[])}.
     */
    @Test
    @Ignore
    public void testDeleteItems() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#deleteItem(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testDeleteItem() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#createNode(java.lang.String, java.lang.String, java.lang.String, org.jivesoftware.smackx.packet.DataForm)}.
     */
    @Test
    @Ignore
    public void testCreateNode() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#configureNode(java.lang.String, java.lang.String, org.jivesoftware.smackx.packet.DataForm)}.
     */
    @Test
    @Ignore
    public void testConfigureNode() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#deleteNode(java.lang.String, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testDeleteNode() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#purgeItems(java.lang.String, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testPurgeItems() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#getSubscriptionList(java.lang.String, java.lang.String, java.lang.String[])}.
     */
    @Test
    @Ignore
    public void testManageSubscriptions() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#manageAffiliations(java.lang.String, java.lang.String, java.lang.String[])}.
     */
    @Test
    @Ignore
    public void testManageAffiliations() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#subscribeCollection(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testSubscribeCollection() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#createCollection(java.lang.String, java.lang.String, org.jivesoftware.smackx.packet.DataForm)}.
     */
    @Test
    @Ignore
    public void testCreateCollection() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#createAssociatedNode(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.jivesoftware.smackx.packet.DataForm)}.
     */
    @Test
    @Ignore
    public void testCreateAssociatedNode() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#associateNodeWithCollection(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testAssociateNodeWithCollection() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#disassociateNodeFromAllCollections(java.lang.String, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testDisassociateNodeFromAllCollections() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#createItemXML(java.lang.String)}.
     */
    @Test
    @Ignore
    public void testCreateItemXMLString() {

        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub.PubSubIQFactory#createItemXML(java.lang.String, java.lang.String)}.
     */
    @Test
    @Ignore
    public void testCreateItemXMLStringString() {

        fail("Not yet implemented");
    }

}
