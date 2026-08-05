package com.peerdrop.desktop.state;

import com.peerdrop.desktop.model.PeerSession;
import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;

public class AppContext {
    private MessageService messageService;
    private FileShareService fileShareService;
    private DiscoveryService discoveryService;

    private Integer messagePort;
    private Integer fileSharePort;
    private PeerSession peerSession;

    public AppContext() {
        reset();
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public FileShareService getFileShareService() {
        return fileShareService;
    }

    public DiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public Integer getMessagePort() {
        return messagePort;
    }

    public void setMessagePort(Integer messagePort) {
        this.messagePort = messagePort;
    }

    public Integer getFileSharePort() {
        return fileSharePort;
    }

    public void setFileSharePort(Integer fileSharePort) {
        this.fileSharePort = fileSharePort;
    }

    public PeerSession getPeerSession() {
        return peerSession;
    }

    public void setPeerSession(PeerSession peerSession) {
        this.peerSession = peerSession;
    }

    public void reset() {
        if (discoveryService != null) discoveryService.close();
        if (fileShareService != null) fileShareService.close();
        if (messageService != null) messageService.close();

        messagePort = null;
        fileSharePort = null;
        peerSession = null;

        messageService = MessageService.create(this);
        fileShareService = FileShareService.create(this);
        discoveryService = DiscoveryService.create(this);
    }
}
