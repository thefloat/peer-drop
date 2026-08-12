/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.viewmodel;

import com.peerdrop.desktop.model.Message;
import com.peerdrop.desktop.model.Peer;
import com.peerdrop.desktop.model.PeerSession;
import com.peerdrop.desktop.protocol.JsonCodec;
import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;
import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.ViewManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CentralHubViewModel}.
 * <p>
 * {@code MessageService.send(...)} and {@code JsonCodec} are called statically
 * from within the view model even though an instance {@code messageService}
 * field also exists, so those calls are mocked via {@code Mockito.mockStatic(...)}
 * in try-with-resources blocks rather than through {@code @Mock} - static
 * mocking has no annotation-based equivalent.
 * <p>
 * Callback paths that route through {@code Platform.runLater} (incoming
 * messages, peer-list updates) aren't covered here to keep these tests
 * independent of the JavaFX toolkit; they'd be good candidates for a
 * TestFX-backed integration test instead.
 */
@ExtendWith(MockitoExtension.class)
class CentralHubViewModelTest {

    @Mock private ViewManager viewManager;
    @Mock private AppContext appContext;
    @Mock private MessageService messageService;
    @Mock private DiscoveryService discoveryService;
    @Mock private FileShareService fileShareService;
    @Mock private PeerSession peerSession;

    private CentralHubViewModel viewModel;

    @BeforeEach
    void setUp() {
        // Consumed by the CentralHubViewModel constructor on every test.
        when(appContext.getMessageService()).thenReturn(messageService);
        when(appContext.getFileShareService()).thenReturn(fileShareService);
        when(appContext.getDiscoveryService()).thenReturn(discoveryService);
        when(appContext.getPeerSession()).thenReturn(peerSession);
        when(peerSession.username()).thenReturn("alice");

        viewModel = new CentralHubViewModel(viewManager, appContext);
    }

    @Test
    void sendMessage_withNoRecipientSelected_isNoOp() {
        viewModel.messageInputProperty().set("hello");

        try (MockedStatic<MessageService> messageServiceStatic = mockStatic(MessageService.class)) {
            viewModel.sendMessage();
            messageServiceStatic.verifyNoInteractions();
        }

        // Bails out before touching state, so the typed text is left in place.
        assertEquals("hello", viewModel.messageInputProperty().get());
    }

    @Test
    void sendMessage_sendsStagedFileThenTextAndClearsState() {
        Peer recipient = mock(Peer.class);
        when(recipient.getUsername()).thenReturn("bob");
        viewModel.selectedNodeProperty().set(recipient);
        viewModel.messageInputProperty().set("hi bob");

        File staged = mock(File.class);
        viewModel.getStagedFiles().add(staged);

        FileShareService.FileMetadata metadata = mock(FileShareService.FileMetadata.class);
        when(fileShareService.hostFile(staged)).thenReturn(metadata);

        try (MockedStatic<JsonCodec> jsonCodec = mockStatic(JsonCodec.class);
             MockedStatic<MessageService> messageServiceStatic = mockStatic(MessageService.class)) {

            jsonCodec.when(() -> JsonCodec.encode(metadata)).thenReturn("{\"file\":\"meta\"}");
            messageServiceStatic.when(() -> MessageService.send(eq(recipient), any(Message.class)))
                    .thenReturn(true);

            viewModel.sendMessage();

            ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
            messageServiceStatic.verify(
                    () -> MessageService.send(eq(recipient), sent.capture()), times(2));

            List<String> sentContents = sent.getAllValues().stream()
                    .map(Message::content)
                    .toList();
            assertTrue(sentContents.contains("{\"file\":\"meta\"}"), "file-offer payload should be sent");
            assertTrue(sentContents.contains("hi bob"), "chat text should be sent");
        }

        assertTrue(viewModel.getStagedFiles().isEmpty());
        assertEquals("", viewModel.messageInputProperty().get());
        assertEquals(2, viewModel.getActiveHistory().size());
    }

    @Test
    void sendFileOffer_withNullFile_neverTouchesFileShareService() {
        Peer recipient = mock(Peer.class);

        viewModel.sendFileOffer(recipient, null);

        verify(fileShareService, never()).hostFile(null);
    }

    @Test
    void getActiveHistory_withNoSelectedPeer_returnsNull() {
        assertNull(viewModel.getActiveHistory());
    }

    @Test
    void leave_resetsAppContextAndNavigatesToGatewayView() {
        viewModel.leave();

        verify(appContext).reset();
        verify(viewManager).reset();
        verify(viewManager).showGatewayView();
    }
}

