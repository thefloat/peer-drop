package com.peerdrop.desktop.service.util;

import java.net.*;
import java.util.*;

public class NetworkInterfaceUtils {

    /*
    Returns null if no viable network interfaces are found.
     */
    public static NetworkInterface selectBestInterface() throws SocketException {
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

        if (viableInterfaces.isEmpty()) { return null; }

        // 1. Attempt to find a wireless interface
        for (NetworkInterface ni : viableInterfaces) {
            if (isLikelyWireless(ni)) { return ni; }
        }

        // 2. Fallback to the first viable interface (often Ethernet)
        NetworkInterface fallback = viableInterfaces.getFirst();
        System.out.println(
                "No wireless found. Using Fallback Interface: " +
                        fallback.getDisplayName() + " (" +
                        fallback.getName() + ")");
        return fallback;
    }

    private static boolean isLikelyWireless(NetworkInterface ni) {
        String name = ni.getName().toLowerCase();
        String display = ni.getDisplayName().toLowerCase();

        return name.contains("wlan") || name.startsWith("wl") ||      // Linux/Android conventions
                display.contains("wi-fi") || display.contains("wireless") || // Windows conventions
                display.contains("airport") ||                         // Older macOS conventions
                name.equals("en0");                                    // Modern macOS (frequently Wi-Fi on MacBooks)
    }
}
