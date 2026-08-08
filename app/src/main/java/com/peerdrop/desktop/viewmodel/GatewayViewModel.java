/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.viewmodel;

import com.peerdrop.desktop.model.PeerSession;
import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;
import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.ViewManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GatewayViewModel {
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty statusLabel = new SimpleStringProperty("");
    private final BooleanProperty isConnecting = new SimpleBooleanProperty(false);

    private final ViewManager viewManager;
    private final AppContext appContext;
    private final MessageService messageService;
    private final FileShareService fileShareService;
    private final DiscoveryService discoveryService;

    public GatewayViewModel(
            ViewManager viewManager,
            AppContext appContext
    ) {
        this.viewManager = viewManager;
        this.appContext = appContext;
        this.messageService = appContext.getMessageService();
        this.fileShareService = appContext.getFileShareService();
        this.discoveryService = appContext.getDiscoveryService();
    }

    private void setPeerSession(String username) {
        appContext.setPeerSession(new PeerSession(username));
    }

    /*
     - One word.
     - >= 2 characters.
    */
    private boolean isValidUsername(String username) {
        if (username.length() < 2) {
            statusLabel.set("username should have length >= 2");
            return false;
        }

        return true;
    }

    public void join() {
        String trimmed = username.get().trim();

        if (trimmed.isEmpty()) {
            statusLabel.set("Error: Username cannot be empty.");
            return;
        }

        isConnecting.set(true);
        statusLabel.set("Connecting...");

        if (!isValidUsername(trimmed)) {
            return;
        }

        setPeerSession(trimmed);

        messageService.start();
        fileShareService.start();
        discoveryService.start();

        viewManager.showCentralHubView();
    }

    // getters
    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty statusMessageProperty() {
        return statusLabel;
    }

    public BooleanProperty isConnectingProperty() {
        return isConnecting;
    }
}
