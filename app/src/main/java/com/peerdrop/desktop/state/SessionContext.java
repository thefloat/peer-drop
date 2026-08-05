package com.peerdrop.desktop.state;

import com.peerdrop.desktop.model.PeerSession;

import java.util.Optional;

public enum SessionContext {
    INSTANCE;

    private Integer messagePort;
    private Integer fileSharePort;
    private PeerSession peerSession;

    public Optional<Integer> getMessagePort() {
        return Optional.of(messagePort);
    }

    public void setMessagePort(int messagePort) {
        this.messagePort = messagePort;
    }

    public Optional<Integer> getFileSharePort() {
        return Optional.of(fileSharePort);
    }

    public void setFileSharePort(int fileSharePort) {
        this.fileSharePort = fileSharePort;
    }

    public Optional<PeerSession> getPeerSession() {
        return Optional.of(peerSession);
    }

    public void setPeerSession(PeerSession peerSession) {
        this.peerSession = peerSession;
    }

    public void clear() {
        messagePort = null;
        fileSharePort = null;
        peerSession = null;
    }
}
