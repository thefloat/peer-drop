/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.service.util;

import java.net.*;
import java.util.*;

public class NetworkInterfaceUtils {

    private static final List<String> WIRELESS_KEYWORDS = List.of(
            "wlan",              // Linux/Android conventions
            "wi-fi", "wireless", // Windows conventions
            "airport",           // Older macOS conventions
            "en0");              // Modern macOS (frequently Wi-Fi on MacBooks)

    /*
    Returns an empty list if no viable network interfaces are found.
     */
    public static List<NetworkInterface> getViableInterfaces() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        List<NetworkInterface> viableInterfaces = new ArrayList<>();

        for (NetworkInterface ni : Collections.list(networkInterfaces)) {
            // Standard viability checks
            if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast()) {
                continue;
            }

            boolean hasIpv4 =
                    ni.inetAddresses().anyMatch(ip -> ip instanceof Inet4Address);

            if (hasIpv4) {
                viableInterfaces.add(ni);
            }
        }

        return viableInterfaces;
    }

    public static boolean isLikelyWireless(NetworkInterface ni) {
        String name = ni.getName().toLowerCase();
        String display = ni.getDisplayName().toLowerCase();

        boolean containsKeyword = WIRELESS_KEYWORDS.stream().anyMatch(
                (k) -> name.contains(k) ||
                        display.contains(k)
        );
        return name.startsWith("wl") || containsKeyword;
    }
}
