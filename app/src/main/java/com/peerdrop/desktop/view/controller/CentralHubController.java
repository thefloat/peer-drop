package com.peerdrop.desktop.view.controller;

import com.peerdrop.desktop.model.Message;
import com.peerdrop.desktop.model.Peer;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.protocol.JsonCodec;
import com.peerdrop.desktop.viewmodel.CentralHubViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FXML controller for the main chat window.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Binding UI controls to {@link CentralHubViewModel} properties</li>
 *   <li>Reacting to list/map changes and refreshing the message/file-staging areas</li>
 *   <li>Building chat-bubble and file-chip nodes on demand</li>
 * </ul>
 *
 * All business logic lives in {@link CentralHubViewModel}; this class is purely presentational.
 */
public class CentralHubController {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final double MAX_FILE_STAGE_HEIGHT = 60.0;
    private static final double BUBBLE_MAX_WIDTH      = 300.0;
    private static final double FILE_CHIP_NAME_WIDTH  = 150.0;
    private static final double FILE_CHIP_SIZE_WIDTH  =  70.0;
    private static final double DOWNLOAD_BAR_WIDTH    = 200.0;
    private static final double BUBBLE_SPACING        =   6.0;
    private static final double FILE_CHIP_SPACING     =   6.0;

    // -------------------------------------------------------------------------
    // FXML-injected fields
    // -------------------------------------------------------------------------

    @FXML private ListView<Peer> peerListView;

    @FXML private Label chatHeaderName;
    @FXML private Label chatHeaderStatus;
    @FXML private Label sessionHandle;

    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox      messageContainer;

    @FXML private ScrollPane fileStageScrollPane;
    @FXML private FlowPane   fileStage;

    @FXML private TextArea messageInput;
    @FXML private Button   btnSend;

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    private final CentralHubViewModel viewModel;

    /** Maps a hosted fileId → its status label so it can be updated on revocation. */
    private final Map<String, Label> fileHostStatuses = new HashMap<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public CentralHubController(CentralHubViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    @FXML
    public void initialize() {
        setupDataBindings();
        setupListeners();
        setupPeerListCellFactory();
        setupActiveChatHistoryListener();
        setupInputAreaEvents();
    }

    /**
     * Binds UI control properties to ViewModel observables so the view
     * stays in sync without explicit update calls.
     */
    private void setupDataBindings() {
        // Session handle
        sessionHandle.textProperty().bind(viewModel.sessionHandleProperty());
        
        // Peer list
        peerListView.setItems(viewModel.getPeers());
        viewModel.selectedNodeProperty()
                 .bind(peerListView.getSelectionModel().selectedItemProperty());

        // Message input
        messageInput.textProperty()
                    .bindBidirectional(viewModel.messageInputProperty());

        // File staging area – show only when files are attached
        BooleanBinding hasAttachments = Bindings.isNotEmpty(viewModel.getStagedFiles());
        fileStageScrollPane.visibleProperty().bind(hasAttachments);
        fileStageScrollPane.managedProperty().bind(hasAttachments);
        fileStageScrollPane.prefHeightProperty().bind(
                Bindings.min(MAX_FILE_STAGE_HEIGHT, fileStage.heightProperty()));

        // Auto-scroll staging pane to bottom when new files are added
        fileStage.heightProperty().addListener(
                _ -> fileStageScrollPane.setVvalue(1.0));

        // Disable Send when no peer is selected
        BooleanBinding noPeerSelected = viewModel.selectedNodeProperty().isNull();
        btnSend.disableProperty().bind(noPeerSelected);
    }

    /**
     * Registers reactive listeners for collections that cannot be handled
     * by simple property bindings.
     */
    private void setupListeners() {
        // Auto-scroll message pane when content grows
        messageContainer.heightProperty().addListener(
                _ -> messageScrollPane.setVvalue(1.0));

        // Keep file-chip strip in sync with the staged-files list
        viewModel.getStagedFiles().addListener(
                (ListChangeListener<? super File>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            change.getAddedSubList()
                                  .forEach(file -> fileStage.getChildren()
                                                            .add(createFileChip(file)));
                        }
                        if (change.wasRemoved()) {
                            // Full sync on removal keeps chip indices consistent
                            syncStagingAreaChips();
                        }
                    }
                });

        // Update the status label when a hosted file is revoked
        viewModel.getHostedFiles().addListener(
                (MapChangeListener<? super String, ? super File>) change -> {
                    if (!change.wasRemoved()) return;
                    Label statusLabel = fileHostStatuses.get(change.getKey());
                    if (statusLabel != null) {
                        statusLabel.setText("Revoked");
                    }
                });
    }

    /** Configures the custom cell renderer for the peer list. */
    private void setupPeerListCellFactory() {
        peerListView.setCellFactory(_ -> new PeerListCell());
    }

    /**
     * Observes {@code selectedNodeProperty} directly. On each peer change:
     * <ol>
     *   <li>The previous message-list listener is detached (using a closure-captured reference).</li>
     *   <li>The message view is cleared and repopulated from {@link CentralHubViewModel#getActiveHistory()}.</li>
     *   <li>A fresh listener is attached to stream in messages that arrive afterwards.</li>
     * </ol>
     */
    private void setupActiveChatHistoryListener() {
        // Both references are mutated on each peer swap; arrays let lambdas close over them.
        ObservableList<Message>[] trackedHistory  = new ObservableList[]{ null };
        ListChangeListener<Message>[] trackedListener = new ListChangeListener[]{ null };

        viewModel.selectedNodeProperty().addListener((observable, oldPeer, newPeer) -> {
            if (trackedHistory[0] != null && trackedListener[0] != null) {
                trackedHistory[0].removeListener(trackedListener[0]);
            }

            messageContainer.getChildren().clear();

            ObservableList<Message> history = viewModel.getActiveHistory();

            if (history == null) {
                showEmptyChatHeader();
                trackedHistory[0]  = null;
                trackedListener[0] = null;
                return;
            }

            updateChatHeader(newPeer);
            history.forEach(this::addMessageBubble);

            ListChangeListener<Message> listener = change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.getAddedSubList().forEach(this::addMessageBubble);
                    }
                }
            };

            trackedHistory[0]  = history;
            trackedListener[0] = listener;
            history.addListener(listener);
        });
    }

    /** Wires Ctrl+Enter to send the current message. */
    private void setupInputAreaEvents() {
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                handleSendMessage();
                event.consume();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Chat-header helpers
    // -------------------------------------------------------------------------

    private void showEmptyChatHeader() {
        chatHeaderName.setText("Select a peer to start chatting");
        chatHeaderStatus.setText("No conversation active");
    }

    private void updateChatHeader(Peer peer) {
        chatHeaderName.setText(peer.getUsername());
        chatHeaderStatus.setText("Active");
    }

    // -------------------------------------------------------------------------
    // Staging-area helpers
    // -------------------------------------------------------------------------

    /** Rebuilds every chip from scratch; used after a removal to keep indices correct. */
    private void syncStagingAreaChips() {
        fileStage.getChildren().clear();
        viewModel.getStagedFiles()
                 .forEach(file -> fileStage.getChildren().add(createFileChip(file)));
    }

    // -------------------------------------------------------------------------
    // FXML action handlers
    // -------------------------------------------------------------------------

    /** Opens a multi-file chooser and stages the selected files. */
    @FXML
    public void handleAttach() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Files to Share");

        List<File> selected = fileChooser.showOpenMultipleDialog(
                messageContainer.getScene().getWindow());

        if (selected != null) {
            viewModel.getStagedFiles().addAll(selected);
        }
    }

    @FXML
    public void handleSendMessage() {
        viewModel.sendMessage();
    }

    @FXML
    public void handleSettings() {
        viewModel.openSettings();
    }

    @FXML
    public void handleLeave() {
        viewModel.leave();
    }

    // -------------------------------------------------------------------------
    // Layout builders – message bubbles
    // -------------------------------------------------------------------------

    /**
     * Creates a chat bubble for {@code msg} and appends it to the message container.
     * File-offer messages receive an interactive download/host widget; plain text
     * messages get a simple label bubble.
     */
    private void addMessageBubble(Message msg) {
        HBox wrapper  = new HBox();
        VBox bubble   = new VBox(BUBBLE_SPACING);
        bubble.setMaxWidth(BUBBLE_MAX_WIDTH);
        boolean incoming = isIncomingMessage(msg);

        wrapper.getStyleClass().add("bubble-wrapper");

        if (incoming) {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            bubble.getStyleClass().add("bubble-incoming");
        } else {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            bubble.getStyleClass().add("bubble-outgoing");
        }

        if (msg.type() == Message.MessageType.FILE_OFFER) {
            populateFileOfferBubble(msg, bubble, incoming);
        } else {
            bubble.getChildren().add(createTextLabel(msg.content()));
        }

        wrapper.getChildren().add(bubble);
        messageContainer.getChildren().add(wrapper);
    }

    private boolean isIncomingMessage(Message msg) {
        Peer peer = viewModel.selectedNodeProperty().get();
        return peer != null && msg.sender().equals(peer.getUsername());
    }

    private Label createTextLabel(String content) {
        Label label = new Label(content);
        label.getStyleClass().add("bubble-text");
        label.setWrapText(true);
        return label;
    }

    // -------------------------------------------------------------------------
    // Layout builders – file-offer bubble
    // -------------------------------------------------------------------------

    /**
     * Populates {@code bubble} with file metadata and either a Download button
     * (for incoming offers) or a "Hosting" status label (for outgoing offers).
     */
    private void populateFileOfferBubble(Message msg, VBox bubble, boolean incoming) {
        FileShareService.FileMetadata metadata =
                JsonCodec.decode(msg.content(), FileShareService.FileMetadata.class);

        bubble.getChildren().addAll(
                createFileNameLabel(metadata.fileName()),
                createFileSizeLabel(metadata.fileSize()));

        if (incoming) {
            bubble.getChildren().addAll(createDownloadControls(msg));
        } else {
            bubble.getChildren().add(createHostStatusLabel(metadata.fileId()));
        }
    }

    private Label createFileNameLabel(String fileName) {
        Label label = new Label("📁 " + fileName);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private Label createFileSizeLabel(long fileSize) {
        Label label = new Label(String.format("%.2f KB", fileSize / 1024.0));
        label.setStyle("-fx-font-size: 11px; -fx-opacity: 0.8;");
        return label;
    }

    /**
     * Returns [downloadButton, progressBar] nodes wired together so that clicking
     * the button hides it, shows the bar, and delegates to the ViewModel.
     */
    private Node[] createDownloadControls(Message msg) {
        Button      btnDownload = new Button("Download");
        ProgressBar progressBar = new ProgressBar(0);

        progressBar.setVisible(false);
        progressBar.setPrefWidth(DOWNLOAD_BAR_WIDTH);

        btnDownload.setOnAction(_ -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Download Destination");
            File destination = chooser.showDialog(messageContainer.getScene().getWindow());

            if (destination != null) {
                btnDownload.setVisible(false);
                progressBar.setVisible(true);
                viewModel.downloadSharedFile(msg, destination, progressBar);
            }
        });

        return new Node[]{ btnDownload, progressBar };
    }

    private Label createHostStatusLabel(String fileId) {
        Label label = new Label("Hosting");
        label.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");
        fileHostStatuses.put(fileId, label);
        return label;
    }

    // -------------------------------------------------------------------------
    // Layout builders – file chip
    // -------------------------------------------------------------------------

    /**
     * Builds a compact chip showing the file name, size, and a remove button
     * for the staging area.
     */
    private Node createFileChip(File file) {
        Label nameLabel = new Label(file.getName());
        nameLabel.getStyleClass().add("file-chip-text");
        nameLabel.setMaxWidth(FILE_CHIP_NAME_WIDTH);

        Label sizeLabel = new Label("(" + getFormattedSize(file.length()) + ")");
        sizeLabel.getStyleClass().add("file-chip-text");
        sizeLabel.setMaxWidth(FILE_CHIP_SIZE_WIDTH);

        Button removeBtn = new Button("×");
        removeBtn.getStyleClass().add("btn-delete");
        removeBtn.setOnAction(_ -> viewModel.getStagedFiles().remove(file));

        HBox chip = new HBox(FILE_CHIP_SPACING, nameLabel, sizeLabel, removeBtn);
        chip.getStyleClass().add("file-chip");
        return chip;
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Converts a raw byte count into a human-readable string (B / KB / MB / …).
     *
     * @param bytes file size in bytes
     * @return formatted string, e.g. {@code "1.4 MB"}
     */
    public String getFormattedSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int  exp    = (int) (Math.log(bytes) / Math.log(1024));
        char prefix = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), prefix);
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    /**
     * Custom {@link ListCell} that renders a peer as an online-dot + username row.
     */
    private static class PeerListCell extends ListCell<Peer> {

        private final HBox   container    = new HBox();
        private final Label  usernameLabel = new Label();

        PeerListCell() {
            Region onlineDot = new Region();
            onlineDot.setMinSize(10, 10);
            onlineDot.setPrefSize(10, 10);
            onlineDot.setMaxSize(10, 10);

            container.getStyleClass().add("peer-cell-container");
            onlineDot.getStyleClass().add("peer-online-dot");
            usernameLabel.getStyleClass().add("peer-username");
            container.getChildren().addAll(onlineDot, usernameLabel);
        }

        @Override
        protected void updateItem(Peer peer, boolean empty) {
            super.updateItem(peer, empty);
            if (empty || peer == null) {
                setGraphic(null);
            } else {
                usernameLabel.setText(peer.getUsername());
                setGraphic(container);
            }
        }
    }
}