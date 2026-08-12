/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.viewmodel;

import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.util.NetworkInterfaceUtils;
import com.peerdrop.desktop.state.AppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SettingsViewModel}.
 * <p>
 * Focused on the actual business logic - loading interfaces and the
 * dirty-check / apply flow - rather than the display-string formatting,
 * which is mostly a thin wrapper around {@link NetworkInterface} getters.
 * <p>
 * {@code NetworkInterfaceUtils.getViableInterfaces()} is a static call, so it's
 * still mocked manually via {@code Mockito.mockStatic(...)} - {@code @Mock}
 * has no static-mocking equivalent.
 */
@ExtendWith(MockitoExtension.class)
class SettingsViewModelTest {

    @Mock private AppContext appContext;
    @Mock private DiscoveryService discoveryService;
    @Mock private NetworkInterface currentInterface;
    @Mock private NetworkInterface otherInterface;

    private MockedStatic<NetworkInterfaceUtils> netUtils;

    @BeforeEach
    void setUp() {
        // Consumed by every test: the constructor always selects currentInterface first
        // and always calls updateDetails() on it.
        when(currentInterface.getInetAddresses())
                .thenReturn(Collections.enumeration(Collections.emptyList()));
        when(appContext.getDiscoveryService()).thenReturn(discoveryService);
        when(appContext.getSelectedNetworkInterface()).thenReturn(currentInterface);

        netUtils = mockStatic(NetworkInterfaceUtils.class);
        netUtils.when(NetworkInterfaceUtils::getViableInterfaces)
                .thenReturn(List.of(currentInterface, otherInterface));
    }

    @AfterEach
    void tearDown() {
        netUtils.close();
    }

    @Test
    void constructor_loadsInterfacesAndPreselectsContextInterface() {
        SettingsViewModel viewModel = new SettingsViewModel(appContext);

        assertEquals(2, viewModel.getAvailableInterfaces().size());
        assertSame(currentInterface, viewModel.selectedInterfaceProperty().get());
        assertFalse(viewModel.isDirtyProperty().get());
    }

    @Test
    void selectingDifferentInterface_marksDirtyAndApplyPersistsAndRestartsDiscovery() {
        // Only this test switches selection to otherInterface, so only it needs the stub.
        when(otherInterface.getInetAddresses())
                .thenReturn(Collections.enumeration(Collections.emptyList()));

        SettingsViewModel viewModel = new SettingsViewModel(appContext);

        viewModel.selectedInterfaceProperty().set(otherInterface);
        assertTrue(viewModel.isDirtyProperty().get());

        viewModel.applyAndSave();

        verify(appContext).setSelectedNetworkInterface(otherInterface);
        verify(discoveryService).close();
        verify(discoveryService).start();
    }

    @Test
    void applyAndSave_whenSelectionMatchesContext_isNoOp() {
        SettingsViewModel viewModel = new SettingsViewModel(appContext);

        // selectedInterface already equals appContext's current interface, so isDirty is false
        viewModel.applyAndSave();

        verify(appContext, never()).setSelectedNetworkInterface(any());
        verifyNoInteractions(discoveryService);
    }
}
