/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.viewmodel;

import com.peerdrop.desktop.model.Message;
import com.peerdrop.desktop.model.PeerSession;
import com.peerdrop.desktop.model.Peer;
import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;
import com.peerdrop.desktop.protocol.JsonCodec;
import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.ProgressBar;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the main chat window.
 *
 * <p>Owns all observable state consumed by {@code CentralHubController} and delegates
 * every network/IO operation to the appropriate service. No JavaFX scene-graph
 * types are referenced here except {@link ProgressBar}, which is passed in from
 * the controller as a progress sink.
 *
 * <p>All mutations that originate on background threads are dispatched back onto
 * the JavaFX Application Thread via {@link Platform#runLater}.
 */
public class CentralHubViewModel {

    // -------------------------------------------------------------------------
    // Observable state
    // -------------------------------------------------------------------------

    private final ObservableList<Peer> peers = FXCollections.observableArrayList();
    private final ObservableMap<String, File> hostedFiles = FXCollections.observableHashMap();
    private final ObservableList<File> stagedFiles = FXCollections.observableArrayList();
    private final ObjectProperty<Peer> selectedNode = new SimpleObjectProperty<>();
    private final StringProperty sessionHandle = new SimpleStringProperty("");
    private final StringProperty messageInput = new SimpleStringProperty("");

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    /** Per-peer message histories; keyed by username. Created lazily on first access. */
    private final Map<String, ObservableList<Message>> messageHistories = new HashMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ViewManager viewManager;
    private final AppContext appContext;
    private final MessageService messageService;
    private final DiscoveryService discoveryService;
    private final FileShareService fileShareService;
    private final PeerSession peerSession;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public CentralHubViewModel(
            ViewManager viewManager,
            AppContext appContext
    ) {
        this.viewManager = viewManager;
        this.appContext = appContext;
        this.messageService = appContext.getMessageService();
        this.fileShareService = appContext.getFileShareService();
        this.discoveryService = appContext.getDiscoveryService();

        this.peerSession = appContext.getPeerSession();
        sessionHandle.set("@" + peerSession.username());

        setupListeners();
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * Wires internal listeners: peer selection → history swap, and the three
     * service callbacks (messages, peer discovery, hosted-file changes).
     */
    private void setupListeners() {
        messageService.subscribe(this::onMessageReceived);
        discoveryService.subscribe(this::onPeerListUpdated);
        fileShareService.addPropertyChangeListener(evt -> {
            if ("hostedFiles".equals(evt.getPropertyName())) {
                onHostedFilesUpdated();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Service callbacks  (all dispatched onto the FX thread)
    // -------------------------------------------------------------------------

    private void onHostedFilesUpdated() {
        Platform.runLater(() -> {
            hostedFiles.clear();
            hostedFiles.putAll(fileShareService.getHostedFiles());
        });
    }

    private void onMessageReceived(Message message) {
        Platform.runLater(() ->
                getOrCreateHistory(message.sender()).add(message));
    }

    private void onPeerListUpdated(List<Peer> updatedPeers) {
        Platform.runLater(() -> peers.setAll(updatedPeers));
    }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Sends all staged files as individual file-offer messages, then sends the
     * typed text (if any), then clears both the staging area and the input field.
     */
    public void sendMessage() {
        Peer   recipient = selectedNode.get();
        String text      = messageInput.get().trim();

        if (recipient == null) return;

        stagedFiles.forEach(file -> sendFileOffer(recipient, file));

        if (!text.isEmpty()) {
            Message chatMessage = new Message(
                    Message.MessageType.CHAT,
                    peerSession.username(),
                    text);
            dispatchMessage(recipient, chatMessage);
        }

        stagedFiles.clear();
        messageInput.set("");
    }

    /**
     * Registers {@code file} with the local HTTP daemon and sends a
     * {@code FILE_OFFER} metadata payload to {@code peer}.
     *
     * @param peer target peer
     * @param file file to share; no-op if {@code null}
     */
    public void sendFileOffer(Peer peer, File file) {
        if (file == null) return;

        FileShareService.FileMetadata metadata = fileShareService.hostFile(file);
        String payload = JsonCodec.encode(metadata);

        Message offerMessage = new Message(
                Message.MessageType.FILE_OFFER,
                peerSession.username(),
                payload);

        dispatchMessage(peer, offerMessage);
    }

    /**
     * Resolves the hosting peer from the offer message, then starts an
     * asynchronous download, reporting progress to {@code progressBar}.
     *
     * @param offerMessage  the original {@code FILE_OFFER} message
     * @param destinationDir directory into which the file will be saved
     * @param progressBar    UI control updated on the FX thread as data arrives
     */
    public void downloadSharedFile(
            Message     offerMessage,
            File        destinationDir,
            ProgressBar progressBar
    ) {
        Peer hostPeer = findPeerBySender(offerMessage.sender());
        if (hostPeer == null) return;

        FileShareService.FileMetadata metadata =
                JsonCodec.decode(offerMessage.content(), FileShareService.FileMetadata.class);

        fileShareService.downloadFile(
                hostPeer.getHost(),
                hostPeer.getFileSharePort(),
                metadata,
                destinationDir,
                buildProgressListener(progressBar));
    }

    public void openSettings() {
        viewManager.openSettingsModal();
    }


    /**
     * Resets AppContext and navigates back to the welcome view.
     */
    public void leave() {
        appContext.reset();
        viewManager.reset();
        viewManager.showGatewayView();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Sends {@code msg} to {@code recipient} via the network layer and, on
     * success, appends it to the local history so it appears in the UI.
     */
    private void dispatchMessage(Peer recipient, Message msg) {
        boolean sent = MessageService.send(recipient, msg);
        if (sent) {
            getOrCreateHistory(recipient.getUsername()).add(msg);
        }
    }

    /**
     * Returns the existing message history for {@code username}, or creates and
     * registers a new empty list if none exists yet.
     */
    private ObservableList<Message> getOrCreateHistory(String username) {
        return messageHistories.computeIfAbsent(
                username, _ -> FXCollections.observableArrayList());
    }

    /**
     * Looks up a connected peer by their username, returning {@code null} if not found.
     */
    private Peer findPeerBySender(String username) {
        return peers.stream()
                    .filter(peer -> peer.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
    }

    /**
     * Builds a {@link FileShareService.ProgressListener} that pipes updates to
     * {@code progressBar} on the JavaFX Application Thread.
     */
    private FileShareService.ProgressListener buildProgressListener(ProgressBar progressBar) {
        return new FileShareService.ProgressListener() {

            @Override
            public void onProgress(double fraction) {
                Platform.runLater(() -> progressBar.setProgress(fraction));
            }

            @Override
            public void onComplete(Path savedPath) {
                Platform.runLater(() ->
                        progressBar.setStyle("-fx-accent: #4CAF82;"));
                System.out.println("File saved to: " + savedPath);
            }

            @Override
            public void onError(Exception e) {
                System.err.println("Download failed: " + e.getMessage());
            }
        };
    }

    // -------------------------------------------------------------------------
    // Property accessors
    // -------------------------------------------------------------------------

    public ObservableList<Peer> getPeers() { return peers;        }
    public ObservableMap<String, File> getHostedFiles() { return hostedFiles;  }
    public ObservableList<File> getStagedFiles() { return stagedFiles;  }
    public ObjectProperty<Peer> selectedNodeProperty() { return selectedNode; }
    public StringProperty sessionHandleProperty() { return sessionHandle; }
    public StringProperty messageInputProperty() { return messageInput; }

    /**
     * Returns the message history for the currently selected peer, or
     * {@code null} if no peer is selected. The returned list is the live
     * observable backing store, so callers can attach their own listeners to it.
     */
    public ObservableList<Message> getActiveHistory() {
        Peer peer = selectedNode.get();
        return peer == null ? null : getOrCreateHistory(peer.getUsername());
    }
}