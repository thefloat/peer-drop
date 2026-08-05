package com.peerdrop.desktop.state;

import com.peerdrop.desktop.model.PeerSession;
import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;

public class AppContext {
    private Integer messagePort;
    private Integer fileSharePort;
    private PeerSession peerSession;

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
}
