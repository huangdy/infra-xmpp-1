/**
 * 
 */

package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

//import org.apache.xmlbeans.XmlObject;

/**
 * Data Services in the IEOC must register IepdListeners for each topic that they manage with each
 * Interest group.
 * 
 * @author roger
 * 
 */
public abstract class IepdListener {

    protected String topic;

    public IepdListener(String topic) {

        this.topic = topic;
    }

    public String getTopic() {

        return topic;
    }

    /**
     * This method will be called when there is an update to the state of a topic. This is the
     * authoritative statement of a change to a topic. Data Services should not use an update
     * directly from a client for any service specific logic but should wait until they receive the
     * IepdChanged callback.
     * 
     * @param update
     *            XML text for an update to the current topic
     */
    public abstract void IepdChanged(String update);

    /**
     * This method will be called when an item is deleted from a topic.
     * 
     * @param uuid
     *            Unique id of the item that was deleted.
     */
    public abstract void deleteItem(String uuid);
}
