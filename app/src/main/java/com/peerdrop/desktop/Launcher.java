package com.peerdrop.desktop;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        Application.launch(App.class, args);
    }
}
