package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;

/**
 * Class to encapsulate the sending of an XMPP stanza (mainly IQs) and wait (given a timeout) for a
 * response based on matching ids. The timeout value is configured by setting the timeout value on
 * the connection that was used to factory this CommandWithReply.
 * 
 * The construction of an instance of this class sends the actual XMPP stanza. Well-formed checks
 * and validation on the stanza should be performed before creating an instance of this class. The
 * response can be obtained using getResult after waitForSuccessOrFailure returns true. If a false
 * result is returned then there was an error returned information about which can be obtained
 * through several accessors.
 * 
 * @author roger
 * 
 */
public interface CommandWithReply {

    public boolean waitForSuccessOrFailure();

    public String getErrorMessage();

    public int getErrorCode();

    public String getErrorCondition();

    public XMPPError getXMPPError();

    public XMPPError.Type getErrorType();

    public IQ getResult();

    public String getSubscriptionID();

    public boolean isInvalidPayload();

    public boolean isItemForbidden();

    public boolean isItemRequired();

    public boolean isPayloadRequired();

    public boolean isPayloadTooBig();

    public boolean isUnsupported();

    public boolean isInvalidJID();

}