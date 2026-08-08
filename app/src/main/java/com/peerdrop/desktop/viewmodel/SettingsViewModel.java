/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.viewmodel;

import com.peerdrop.desktop.service.DiscoveryService;
import com.peerdrop.desktop.service.util.NetworkInterfaceUtils;
import com.peerdrop.desktop.state.AppContext;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class SettingsViewModel {

    // Dependencies (Inject Network/Peer Service here)
    private final AppContext appContext;
    private final DiscoveryService discoveryService;

    // Collections & Selection
    private final ObservableList<NetworkInterface> availableInterfaces = FXCollections.observableArrayList();
    private final ObjectProperty<NetworkInterface> selectedInterface = new SimpleObjectProperty<>();

    // Detail Properties (UI Data Bindings)
    private final StringProperty displayName = new SimpleStringProperty("-");
    private final StringProperty interfaceId = new SimpleStringProperty("-");
    private final StringProperty ipAddresses = new SimpleStringProperty("-");
    private final StringProperty macAddress = new SimpleStringProperty("-");
    private final StringProperty mtuValue = new SimpleStringProperty("-");
    private final StringProperty statusFlags = new SimpleStringProperty("-");

    // Dirty state property for Apply button activation
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    public SettingsViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.discoveryService = appContext.getDiscoveryService();

        loadNetworkInterfaces();

        isDirty.bind(Bindings.createBooleanBinding(
                () -> selectedInterface.get() != appContext.getSelectedNetworkInterface(),
                selectedInterface
        ));

        // Update display details when selection changes
        updateDetails(selectedInterface.get());
        selectedInterface.addListener(
                (obs, oldVal, newVal) -> updateDetails(newVal));

    }

    private void loadNetworkInterfaces() {
        try {
            var networkInterfaces = NetworkInterfaceUtils.getViableInterfaces();
            availableInterfaces.setAll(networkInterfaces);

            selectedInterface.set(appContext.getSelectedNetworkInterface());
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateDetails(NetworkInterface networkInterface) {
        if (networkInterface == null) {
            clearDetails();
            return;
        }

        try {
            displayName.set(networkInterface.getDisplayName());
            interfaceId.set(networkInterface.getName());

            // Extract IPs
            StringBuilder ips = new StringBuilder();
            for (InetAddress addr : Collections.list(networkInterface.getInetAddresses())) {
                if (!ips.isEmpty()) ips.append("\n");
                ips.append(addr.getHostAddress());
            }
            ipAddresses.set(!ips.isEmpty() ? ips.toString() : "None");

            // Hardware MAC Address
            byte[] mac = networkInterface.getHardwareAddress();
            if (mac != null) {
                StringBuilder macSb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    macSb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                }
                macAddress.set(macSb.toString());
            } else {
                macAddress.set("N/A (Virtual / Loopback)");
            }

            // MTU & Flags
            mtuValue.set(String.valueOf(networkInterface.getMTU()));

            StringBuilder flags = new StringBuilder();
            if (networkInterface.isUp()) flags.append("UP ");
            if (networkInterface.isLoopback()) flags.append("LOOPBACK ");
            if (networkInterface.isVirtual()) flags.append("VIRTUAL ");
            if (networkInterface.supportsMulticast()) flags.append("MULTICAST ");
            statusFlags.set(flags.toString().trim());

        } catch (SocketException e) {
            clearDetails();
            displayName.set("Error loading interface details");
        }
    }

    private void clearDetails() {
        displayName.set("-");
        interfaceId.set("-");
        ipAddresses.set("-");
        macAddress.set("-");
        mtuValue.set("-");
        statusFlags.set("-");
    }

    public void applyAndSave() {
        if (!isDirty.get()) return;

        NetworkInterface newInterface = selectedInterface.get();
        appContext.setSelectedNetworkInterface(newInterface);

        // Trigger discovery service restart
        discoveryService.close();
        discoveryService.start();
    }

    // --- Property Getters ---
    public ObservableList<NetworkInterface> getAvailableInterfaces() { return availableInterfaces; }
    public ObjectProperty<NetworkInterface> selectedInterfaceProperty() { return selectedInterface; }
    public BooleanProperty isDirtyProperty() { return isDirty; }

    public StringProperty displayNameProperty() { return displayName; }
    public StringProperty interfaceIdProperty() { return interfaceId; }
    public StringProperty ipAddressesProperty() { return ipAddresses; }
    public StringProperty macAddressProperty() { return macAddress; }
    public StringProperty mtuValueProperty() { return mtuValue; }
    public StringProperty statusFlagsProperty() { return statusFlags; }
}