/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        Application.launch(App.class, args);
    }
}
