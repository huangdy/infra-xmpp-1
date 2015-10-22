package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import com.leidos.xchangecore.core.infrastructure.messages.CoreRosterMessage;
import com.leidos.xchangecore.core.infrastructure.messages.CoreStatusUpdateMessage;
import com.leidos.xchangecore.core.infrastructure.messages.PublishProductMessage;

public class XmppMessageReceiver {

    private int getInterestGroupDocumentRequestMessagesReceived;

    private int getProductRequestMessagesReceived;

    private int CoreRosterMessagesReceived;

    private int CoreStatusUpdateMessagesReceived;

    private int publishProductMessagesReceived;

    public XmppMessageReceiver() {

        clear();
    }

    public void clear() {

        setGetInterestGroupDocumentRequestMessagesReceived(0);
        setGetProductRequestMessagesReceived(0);
        setCoreRosterMessagesReceived(0);
        setCoreStatusUpdateMessagesReceived(0);
        setPublishProductMessagesReceived(0);
    }

    public void coreRosterHandler(CoreRosterMessage message) {

        setCoreRosterMessagesReceived(CoreRosterMessagesReceived + 1);
    }

    public void coreStatusUpdateHandler(CoreStatusUpdateMessage message) {

        setCoreStatusUpdateMessagesReceived(CoreStatusUpdateMessagesReceived + 1);
    }

    public int getCoreRosterMessagesReceived() {

        return CoreRosterMessagesReceived;
    }

    public int getCoreStatusUpdateMessagesReceived() {

        return CoreStatusUpdateMessagesReceived;
    }

    public int getGetInterestGroupDocumentRequestMessagesReceived() {

        return getInterestGroupDocumentRequestMessagesReceived;
    }

    public int getGetProductRequestMessagesReceived() {

        return getProductRequestMessagesReceived;
    }

    public int getPublishProductMessagesReceived() {

        return publishProductMessagesReceived;
    }

    public void publishProductHandler(PublishProductMessage msg) {

        setPublishProductMessagesReceived(publishProductMessagesReceived + 1);
    }

    public void setCoreRosterMessagesReceived(int coreRosterMessagesReceived) {

        CoreRosterMessagesReceived = coreRosterMessagesReceived;
    }

    public void setCoreStatusUpdateMessagesReceived(int coreStatusUpdateMessagesReceived) {

        CoreStatusUpdateMessagesReceived = coreStatusUpdateMessagesReceived;
    }

    public void setGetInterestGroupDocumentRequestMessagesReceived(int getInterestGroupDocumentRequestMessagesReceived) {

        this.getInterestGroupDocumentRequestMessagesReceived = getInterestGroupDocumentRequestMessagesReceived;
    }

    public void setGetProductRequestMessagesReceived(int getProductRequestMessagesReceived) {

        this.getProductRequestMessagesReceived = getProductRequestMessagesReceived;
    }

    public void setPublishProductMessagesReceived(int publishProductMessagesReceived) {

        this.publishProductMessagesReceived = publishProductMessagesReceived;
    }
}
