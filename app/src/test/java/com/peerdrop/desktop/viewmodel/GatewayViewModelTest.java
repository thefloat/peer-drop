/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.viewmodel;

import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.FileShareService;
import com.peerdrop.desktop.service.MessageService;
import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.view.ViewManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GatewayViewModel}.
 * <p>
 * These tests only exercise {@code join()}, since that's the single piece of
 * real logic in this view model - everything else is plain property plumbing.
 */
@ExtendWith(MockitoExtension.class)
class GatewayViewModelTest {

    @Mock private ViewManager viewManager;
    @Mock private AppContext appContext;
    @Mock private MessageService messageService;
    @Mock private FileShareService fileShareService;
    @Mock private DiscoveryService discoveryService;

    private GatewayViewModel viewModel;

    @BeforeEach
    void setUp() {
        // Consumed by the GatewayViewModel constructor on every test.
        when(appContext.getMessageService()).thenReturn(messageService);
        when(appContext.getFileShareService()).thenReturn(fileShareService);
        when(appContext.getDiscoveryService()).thenReturn(discoveryService);

        viewModel = new GatewayViewModel(viewManager, appContext);
    }

    @Test
    void join_withBlankUsername_setsErrorAndNeverStartsServices() {
        viewModel.usernameProperty().set("   ");

        viewModel.join();

        assertEquals("Error: Username cannot be empty.", viewModel.statusMessageProperty().get());
        verifyNoInteractions(messageService, fileShareService, discoveryService);
        verify(viewManager, never()).showCentralHubView();
        assertFalse(viewModel.isConnectingProperty().get());
    }

    @Test
    void join_withTooShortUsername_failsValidationAndDoesNotStartServices() {
        viewModel.usernameProperty().set("x");

        viewModel.join();

        assertEquals("username should have length >= 2", viewModel.statusMessageProperty().get());
        verifyNoInteractions(messageService, fileShareService, discoveryService);
        verify(viewManager, never()).showCentralHubView();

        assertTrue(viewModel.isConnectingProperty().get());
    }

    @Test
    void join_withValidUsername_startsAllServicesAndNavigatesToHub() {
        viewModel.usernameProperty().set("  Neo  ");

        viewModel.join();

        verify(appContext).setPeerSession(argThat(session -> "Neo".equals(session.username())));
        verify(messageService).start();
        verify(fileShareService).start();
        verify(discoveryService).start();
        verify(viewManager).showCentralHubView();
        assertEquals("Connecting...", viewModel.statusMessageProperty().get());
    }
}

