/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.state;

import com.peerdrop.desktop.model.PeerSession;
import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;

import java.net.NetworkInterface;

public class AppContext {
    private MessageService messageService;
    private FileShareService fileShareService;
    private DiscoveryService discoveryService;

    private PeerSession peerSession;
    private Integer messagePort;
    private Integer fileSharePort;
    private NetworkInterface selectedNetworkInterface;

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

    public PeerSession getPeerSession() {
        return peerSession;
    }

    public void setPeerSession(PeerSession peerSession) {
        this.peerSession = peerSession;
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

    public NetworkInterface getSelectedNetworkInterface() {
        return selectedNetworkInterface;
    }

    public void setSelectedNetworkInterface(NetworkInterface selectedNetworkInterface) {
        this.selectedNetworkInterface = selectedNetworkInterface;
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
