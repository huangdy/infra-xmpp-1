package com.leidos.xchangecore.core.infrastructure.xmpp.extensions.pubsub;

import java.util.UUID;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import com.leidos.xchangecore.core.infrastructure.xmpp.extensions.util.ArbitraryIQ;

/**
 * This class provides a static factory for creating custom IQ packets relating to the <a
 * href="http://www.xmpp.org/extensions/xep-0060.html"> XMPP Publish-Subscribe protocol</a>
 * (XEP-60). <br />
 * The methods reflect the use cases provided by the XEP-60 spec.
 * 
 * <pre>
 * ConnectionConfiguration config = null;
 * XMPPConnection con = null;
 * IQ iq = null;
 * 
 * // establish connection to server and login
 * config = new ConnectionConfiguration(&quot;xmpp.domain.com&quot;, 5222);
 * con = new XMPPConnection(config);
 * con.connect();
 * con.login(&quot;username&quot;, &quot;password&quot;);
 * 
 * // the address of the pubsub component
 * String pubsubsvc = &quot;pubsub.xmpp.domain.com&quot;;
 * 
 * iq = PubSubIQFactory.discoverInfo(pubsubsvc);
 * con.sendPacket(iq);
 * 
 * iq = PubSubIQFactory.createNode(pubsubsvc, &quot;/myNode&quot;, PubSubConstants.LEAF_NODE, null);
 * con.sendPacket(iq);
 * 
 * iq = PubSubIQFactory.subscribeNode(pubsubsvc, &quot;user@xmpp.server&quot;, &quot;/myNode&quot;, null);
 * con.sendPacket(iq);
 * 
 * iq = PubSubIQFactory.publishItem(pubsubsvc, &quot;/myNode&quot;, createItemXML(&quot;&lt;myXML /&gt;&quot;));
 * con.sendPacket(iq);
 * 
 * </pre>
 * 
 * @see org.jivesoftware.smack.packet.IQ
 * @see org.jivesoftware.smack.XMPPConnection
 */

public class PubSubIQFactory {

    /**
     * Entity Use Case 5.1, 5.3, 5.4 <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#entity-features">Discover Features</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @return an IQ packet configured to...
     */
    public static IQ discoverInfo(String svc, String nodeName) {

        DiscoverInfo iq = new DiscoverInfo();
        iq.setTo(svc);

        if (nodeName == null) {
            return iq;
        } else {
            iq.setNode(nodeName);
        }

        return iq;
    }

    /**
     * Entity Use Case 5.2, 5.5 <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#entity-nodes">Discover Nodes</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @return an IQ packet configured to...
     */
    public static IQ discoverItems(String svc, String nodeName) {

        DiscoverItems iq = new DiscoverItems();
        iq.setTo(svc);

        if (nodeName == null) {
            return iq;
        } else {
            iq.setNode(nodeName);
        }

        return iq;
    }

    /**
     * Entity Use Case 5.6 <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#entity-subscriptions">Retrive
     * Subscriptions</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @return an IQ packet configured to...
     */
    public static IQ retrieveSubscriptions(String svc) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();

        iq.setType(IQ.Type.GET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<subscriptions />");
        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Entity Use Case 5.7 <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#entity-affiliations">Retrieve
     * Affiliations</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @return an IQ packet configured to...
     */
    public static IQ retrieveAffiliations(String svc) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.GET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<affiliations />");
        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    // Subscriber Use Cases

    /**
     * Subscriber Use Case 6.1, 9.1, 9.2 <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#subscriber-subscribe">Subscribe to a
     * Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param jid
     *            a valid JID - eg: user@server.com, user@server.com/office, comp.server.com
     * @param xData
     * @return an IQ packet configured to...
     */
    public static IQ subscribeNode(String svc, String jid, String nodeName, DataForm xData) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");

        sb.append("<subscribe");
        if (nodeName != null)
            sb.append(" node='" + nodeName + "'");
        sb.append(" jid='" + jid + "'");
        sb.append(" />");

        if (xData != null) {
            sb.append("<options>");
            sb.append(xData.toXML());
            sb.append("</options>");
        }

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Subscriber Use Case 6.2 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#subscriber-unsubscribe">Unsubscribe from a
     * Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param jid
     *            a valid JID - eg: user@server.com, user@server.com/office, comp.server.com
     * @param subid
     *            a server assigned subscription id or null. an entity's subids can be retrieved via
     *            retrieveSubscriptions()
     * @return an IQ packet configured to...
     */
    public static IQ unsubscribeNode(String svc, String jid, String nodeName, String subid) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");

        sb.append("<unsubscribe");
        sb.append(" jid='" + jid + "'");
        if (nodeName != null)
            sb.append(" node='" + nodeName + "'");
        if (subid != null)
            sb.append(" subid='" + subid + "'/>");
        sb.append(" />");

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Subscriber Use Case 6.3 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#subscriber-configure">Configure
     * Subscription Options</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param jid
     *            a valid JID - eg: user@server.com, user@server.com/office, comp.server.com
     * @param xData
     *            an xmpp jabber:x:data snippet
     * @param subID
     *            a server assigned subscription id or null. an entity's subids can be retrieved via
     *            retrieveSubscriptions()
     * @return an IQ packet configured to...
     */
    public static IQ configureSubscriptionOptions(String svc, String jid, String nodeName,
            String subID, DataForm xData) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");

        sb.append("<options");
        sb.append(" jid='" + jid + "'>");
        if (nodeName != null)
            sb.append(" node='" + nodeName + "'");
        if (subID != null)
            sb.append(" subid='" + subID + "'");
        sb.append(">");

        if (xData != null)
            sb.append(xData.toXML());

        sb.append("</options>");
        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Subscriber Use Case 6.4 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#subscriber-retrieve">Retrieve Items from a
     * Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param idList
     *            a string array of item IDs
     * @param subID
     *            a server assigned subscription id or null. an entity's subids can be retrieved via
     *            retrieveSubscriptions()
     * @return an IQ packet configured to...
     */
    public static IQ retrieveItems(String svc, String nodeName, String subID, String[] idList) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.GET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");

        sb.append("<items");
        if (nodeName != null)
            sb.append(" node='" + nodeName + "'");
        if (subID != null)
            sb.append(" subid='" + subID + "'");
        sb.append(" >");

        if (idList != null) {
            for (String tmpID : idList) {
                sb.append("<item id='" + tmpID + "'/>");
            }
        }

        sb.append("</items>");

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;

    }

    /**
     * Subscriber Use Case 6.4 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#subscriber-retrieve">Retrieve Items from a
     * Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param itemID
     *            a single item id
     * @param subID
     *            a server assigned subscription id or null. an entity's subids can be retrieved via
     *            retrieveSubscriptions()
     * @return an IQ packet configured to...
     */
    public static IQ retrieveItem(String svc, String nodeName, String subID, String itemID) {

        String[] idList = { itemID };

        return retrieveItems(svc, nodeName, subID, idList);
    }

    // Publisher Use Cases

    /**
     * Publisher Use Case 7.1 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#publisher-publish">Publish an Item to a
     * Node</a> <br />
     * <br />
     * Note: the item wrapper can be created via createItemXML(String xml)
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param itemXML
     *            an xml snippet to be published, must have <item id=''/> as the root element
     * @return an IQ packet configured to...
     */
    public static IQ publishItem(String svc, String nodeName, String itemXML) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<publish node='" + nodeName + "'>");
        sb.append(itemXML);
        sb.append("</publish>");
        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Publisher Use Case 7.2 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#publisher-delete">Delete an Item to a
     * Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param idList
     *            an array of item IDs to be retracted
     * @return an IQ packet configured to...
     */
    public static IQ deleteItems(String svc, String nodeName, String[] idList) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<retract node='" + nodeName + "'>");

        for (String tmpID : idList) {
            sb.append("<item id='" + tmpID + "'/>");
        }

        sb.append("</retract>");
        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Publisher Use Case 7.2 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#publisher-delete">Delete an Item to a
     * Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param itemID
     *            a single item ID to be retracted
     * @return an IQ packet configured to...
     */
    public static IQ deleteItem(String svc, String nodeName, String itemID) {

        String[] idList = { itemID };

        return deleteItems(svc, nodeName, idList);
    }

    /**
     * Owner Use Case 8.1.2 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#owner-create-default">Create a Node with
     * Default Configuration</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param nodeType
     *            either PubSubConstants.COLLECTION_NODE or PubSubConstants.LEAF_NODE
     * @param xData
     * @return an IQ packet configured to...
     */

    public static IQ createNode(String svc, String nodeName, String nodeType, DataForm xData) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        if (xData == null) {
            xData = new DataForm("submit");
            FormField formTypeField = new FormField("FORM_TYPE");
            formTypeField.setType("hidden");
            formTypeField.addValue("http://jabber.org/protocol/pubsub#node_config");
            xData.addField(formTypeField);
        }

        if (nodeType.equals(PubSubConstants.COLLECTION_NODE)) {
            FormField nodeTypeField = new FormField("pubsub#node_type");
            nodeTypeField.addValue("collection");
            xData.addField(nodeTypeField);
        } else {
            FormField nodeTypeField = new FormField("pubsub#node_type");
            nodeTypeField.addValue(PubSubConstants.LEAF_NODE);
            xData.addField(nodeTypeField);
        }

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<create node='" + nodeName + "'/>");

        sb.append("<configure>");
        sb.append(xData.toXML());
        sb.append("</configure>");

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Owner Use Case 8.2 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#owner-configure">Configure a Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param xData
     *            an xmpp jabber:x:data snippet
     * @return an IQ packet configured to...
     */
    public static IQ configureNode(String svc, String nodeName, DataForm xData) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");

        if (xData == null) {
            sb.append("<configure node='" + nodeName + "' />");
        } else {
            sb.append("<configure node='" + nodeName + "'>");
            sb.append(xData.toXML());
            sb.append("</configure>");
        }

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Owner Use Case 8.4 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#owner-delete">Delete a Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @return an IQ packet configured to...
     */
    public static IQ deleteNode(String svc, String nodeName) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub#owner'>");

        sb.append("<delete node='" + nodeName + "'/>");

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Owner Use Case 8.5 - <a href="http://www.xmpp.org/extensions/xep-0060.html#owner-purge">Purge
     * a Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @return an IQ packet configured to...
     */
    public static IQ purgeItems(String svc, String nodeName) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub#owner'>");

        sb.append("<purge node='" + nodeName + "'/>");

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Owner Use Case 8.7 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#owner-subscriptions">Manage
     * Subscriptions</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param subscriptions
     * @return an IQ packet configured to...
     */
    public static IQ getSubscriptionList(String svc, String nodeName) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.GET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub#owner'>");

        sb.append("<subscriptions node='" + nodeName + "'>");

        sb.append("</subscriptions>");

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    /**
     * Owner Use Case 8.8 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#owner-affiliations">Manage
     * Affiliations</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param affiliations
     * @return an IQ packet configured to...
     */
    public static IQ manageAffiliations(String svc, String nodeName, String[] affiliations) {

        StringBuffer sb = new StringBuffer();
        ArbitraryIQ iq = new ArbitraryIQ();
        iq.setType(IQ.Type.SET);
        iq.setTo(svc);

        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub#owner'>");

        sb.append("<affiliations node='" + nodeName + "'>");

        if (affiliations != null) {
            for (String affiliation : affiliations) {
                sb.append(affiliation);
            }
        }

        sb.append("</affiliations>");

        sb.append("</pubsub>");

        iq.setChildElementXML(sb.toString());

        return iq;
    }

    // Collection Nodes, COMING SOON!!

    /**
     * Collection Use Case 9.1, 9.2 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#collections-subscribe">Subscribe to a
     * Collection Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param jid
     * @param subType
     *            either PubSubIQConstants.NODES_SUBSCRIPTION or
     *            PubSubIQConstants.ITEMS_SUBSCRIPTION
     * @param subDepth
     *            "all" or an integer value > 0
     * @return an IQ packet configured to...
     */
    // 9.3
    public static IQ subscribeCollection(String svc, String jid, String nodeName, String subType,
            String subDepth) {

        DataForm xData = new DataForm("submit");

        FormField formTypeField = new FormField("FORM_TYPE");
        formTypeField.setType("hidden");
        formTypeField.addValue("http://jabber.org/protocol/pubsub#subscribe_options");

        FormField subTypeField = new FormField("pubsub#subscription_type");
        subTypeField.addValue(subType);

        FormField subDepthField = new FormField("pubsub#subscription_depth");
        subDepthField.addValue(subDepth);
        xData.addField(formTypeField);
        xData.addField(subTypeField);
        xData.addField(subDepthField);

        return subscribeNode(svc, jid, nodeName, xData);
    }

    /**
     * Collection Use Case 9.3 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#collections-createnode">Create a New
     * Collection Node</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param xData
     * @return an IQ packet configured to...
     */
    // 9.3
    public static IQ createCollection(String svc, String nodeName, DataForm xData) {

        return createNode(svc, nodeName, PubSubConstants.COLLECTION_NODE, xData);
    }

    /**
     * Collection Use Case 9.4 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#collections-createassociated">Create a
     * Node Associated with a Collection</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name
     * @param collectionName
     *            valid collection name
     * @param nodeType
     *            either PubSubIQConstants.COLLECTION_NODE or PubSubIQConstants.LEAF_NODE
     * @param xData
     * @return an IQ packet configured to...
     */
    public static IQ createAssociatedNode(String svc, String collectionName, String nodeName,
            String nodeType, DataForm xData) {

        if (xData == null) {
            xData = new DataForm("submit");
            // FormField formTypeField = new FormField("FORM_TYPE");
        }

        FormField associationField = new FormField("pubsub#collection");
        associationField.addValue(collectionName);
        xData.addField(associationField);

        return createNode(svc, nodeName, nodeType, xData);

    }

    /**
     * Collection Use Case 9.5 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#collections-associate">Associate an
     * Existing Node with a Collection</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @param collectionName
     *            a valid collection name
     * @return an IQ packet configured to...
     */
    public static IQ associateNodeWithCollection(String svc, String collectionName, String nodeName) {

        DataForm xData = new DataForm("submit");
        FormField formTypeField = new FormField("FORM_TYPE");
        formTypeField.setType("hidden");
        formTypeField.addValue("http://jabber.org/protocol/pubsub#node_config");

        FormField associationField = new FormField("pubsub#collection");
        associationField.addValue(collectionName);
        xData.addField(associationField);

        return configureNode(svc, nodeName, xData);

    }

    /**
     * Collection Use Case 9.6 - <a
     * href="http://www.xmpp.org/extensions/xep-0060.html#collections-disassociate">Disassociate a
     * Node from all Collections</a>
     * 
     * @param svc
     *            the address of the pubsub component - eg: pubsub.myserver.net
     * @param nodeName
     *            valid node name or null for the root node
     * @return an IQ packet configured to...
     */
    public static IQ disassociateNodeFromAllCollections(String svc, String nodeName) {

        DataForm xData = new DataForm("submit");
        FormField formTypeField = new FormField("FORM_TYPE");
        formTypeField.setType("hidden");
        formTypeField.addValue("http://jabber.org/protocol/pubsub#node_config");

        FormField associationField = new FormField("pubsub#collection");
        associationField.addValue("");
        xData.addField(associationField);

        return configureNode(svc, nodeName, xData);
    }

    // --------- Utility methods ----------------------

    /**
     * Utility method for wrapping an ArbitraryIQ piece of XML with the <item id=''/> tag. An uuid
     * is automatically generated for the id.
     * 
     * @param xml
     *            any valid xml data - this is not validated!
     * @return an IQ packet configured to...
     */
    public static String createItemXML(String xml) {

        String itemXML = "<item id='" + UUID.randomUUID().toString() + "'>" + xml + "</item>";

        return itemXML;
    }

    /**
     * Utility method for wrapping an ArbitraryIQ piece of XML with the <item id=''/> tag. An uuid
     * is automatically generated for the id.
     * 
     * @param xml
     *            any valid xml data - this is not validated!
     * @param uuid
     *            the item id of the item (used for updating the item)
     * @return an IQ packet configured to...
     */
    public static String createItemXML(String xml, String uuid) {

        String itemXML = null;
        itemXML = "<item id='" + uuid + "'>" + xml + "</item>";

        return itemXML;
    }

}
