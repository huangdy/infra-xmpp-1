package com.leidos.xchangecore.core.infrastructure.xmpp.utils;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.XMPPError.Type;

import com.leidos.xchangecore.core.infrastructure.xmpp.communications.CommandWithReply;

/**
 * This class can be used as a CommandWithReplay test double in conjunction with the
 * DataDrivenCoreConnection class. The sucess and result can be set and added to the
 * DataDrivenCoreConnection. Then when clients call createCommandWithReply on the
 * DataDrivenCoreConnection the reply that matches the incoming packet id will be returned.
 * 
 * @author roger
 * 
 */
public class MockCommandWithReply
    implements CommandWithReply {

    private boolean success = false;
    private IQ result;

    public void setSuccess(boolean success) {

        this.success = success;
    }

    public void setResult(IQ result) {

        this.result = result;
    }

    @Override
    public int getErrorCode() {

        return 0;
    }

    @Override
    public String getErrorCondition() {

        return null;
    }

    @Override
    public String getErrorMessage() {

        return null;
    }

    @Override
    public Type getErrorType() {

        return null;
    }

    @Override
    public IQ getResult() {

        return result;
    }

    @Override
    public String getSubscriptionID() {

        return null;
    }

    @Override
    public boolean isInvalidJID() {

        return false;
    }

    @Override
    public boolean isInvalidPayload() {

        return false;
    }

    @Override
    public boolean isItemForbidden() {

        return false;
    }

    @Override
    public boolean isItemRequired() {

        return false;
    }

    @Override
    public boolean isPayloadRequired() {

        return false;
    }

    @Override
    public boolean isPayloadTooBig() {

        return false;
    }

    @Override
    public boolean isUnsupported() {

        return false;
    }

    @Override
    public boolean waitForSuccessOrFailure() {

        return success;
    }

    @Override
    public XMPPError getXMPPError() {

        // TODO Auto-generated method stub
        return null;
    }
}
