package com.leidos.xchangecore.core.infrastructure.xmpp.communications;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

public class InterestGroupFileManager {

    private Logger logger = Logger.getLogger(this.getClass());

    private FileTransferManager fileTransferManager = null;

    public InterestGroupFileManager(CoreConnectionImpl connection) {

        fileTransferManager = connection.getFileTransferManager();

        // Add a listener to handle incoming file transfer requests
        fileTransferManager.addFileTransferListener(new FileTransferListener() {

            public void fileTransferRequest(final FileTransferRequest request) {

                new Thread(new Runnable() {

                    public void run() {

                        IncomingFileTransfer transfer = request.accept();
                        logger.info("InterestGroupFileManager accepting file " +
                                    request.getDescription());

                        try {
                            URI uri = new URI(request.getDescription());
                            File file = new File(uri);

                            // Create the directory if it doesn't exist
                            File dir = file.getParentFile();
                            if (dir != null && !dir.exists()) {
                                if (!dir.mkdirs()) {
                                    logger.error("FileTransferManager cannot create directory " +
                                                 dir.getName());
                                }
                            }

                            // Transfer the file (overwrite if exists)
                            try {
                                transfer.recieveFile(file);
                            } catch (XMPPException e) {
                                logger.error("InterestGroupFileManager error transfering file " +
                                             request.getFileName() + " : " + e.getMessage());
                                return;
                            }
                        } catch (URISyntaxException e) {
                            logger.error("InterestGroupFileManager:fileTransferRequest error constructing URI from " +
                                         request.getDescription() + " : " + e.getMessage());
                        }
                    }
                }).start();
            }

        });
    }

    public void transferFileToCore(String jid, String fileName) {

        logger.info("InterestGroupFileManager sending file " + fileName + " to " + jid);
        OutgoingFileTransfer outgoing = fileTransferManager.createOutgoingFileTransfer(jid);

        URI uri;
        try {
            uri = new URI(fileName);
            File file = new File(uri);
            if (file.exists()) {
                try {
                    outgoing.sendFile(file, fileName);
                } catch (XMPPException e) {
                    logger.error("InterestGroupFileManager:transferFileToCore error " +
                                 e.getMessage());
                } catch (IllegalArgumentException e) {
                    logger.error("InterestGroupFileManager:transferFileToCore error " +
                                 e.getMessage());
                }
            } else {
                logger.error("InterestGroupFileManager:transferFileToCore file " + fileName +
                             " does not exist.");
            }
        } catch (URISyntaxException e) {
            logger.error("InterestGroupFileManager:transferFileToCore error constructing URI from " +
                         fileName + " : " + e.getMessage());
        }
    }
}
