/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.service;

import com.peerdrop.desktop.state.AppContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.NetworkInterface;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * NOTE: DiscoveryService binds directly to a real MulticastSocket on a hardcoded
 * port (8888) and enumerates real network interfaces, which makes it hard to unit
 * test in true isolation without refactoring for dependency injection. The tests
 * below either avoid touching the network entirely, or use JUnit Assumptions to
 * skip gracefully in sandboxed/CI environments where multicast isn't available,
 * rather than letting the build flake.
 */
@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private AppContext appContext;

    @Test
    void servicePorts_exposesMessageAndFileSharePorts() {
        DiscoveryService.ServicePorts ports = new DiscoveryService.ServicePorts(5000, 6000);

        assertEquals(5000, ports.messagePort());
        assertEquals(6000, ports.fileSharePort());
    }

    @Test
    void close_beforeStart_doesNotThrow() {
        DiscoveryService service = DiscoveryService.create(appContext);

        assertDoesNotThrow(service::close);
    }

    @Test
    void start_isIdempotent_secondCallDoesNotAttemptToRebindSocket() {
        NetworkInterface loopback;
        try {
            loopback = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        } catch (Exception e) {
            loopback = null;
        }
        // Bypass NetworkInterfaceUtils' real interface-selection logic entirely by
        // handing the service a known-good loopback interface via AppContext.
        Assumptions.assumeTrue(loopback != null, "No usable loopback interface in this environment");
        when(appContext.getSelectedNetworkInterface()).thenReturn(loopback);

        DiscoveryService service = DiscoveryService.create(appContext);

        try {
            service.start();
        } catch (RuntimeException e) {
            Assumptions.abort("Multicast not available in this environment: " + e.getMessage());
        }

        // If start() didn't guard on isRunning, this second call would try to init()
        // (and bind the multicast port) again and throw a BindException.
        assertDoesNotThrow(service::start);

        service.close();
    }
}
