package com.peerdrop.desktop.model;

public class Peer {

    private final String username;
    private final String host;
    private final int messagePort;
    private final int fileSharePort;

    private long lastSeen;


    public Peer(String username, String host, int messagePort, int fileSharePort) {
        this.username = username;
        this.host = host;
        this.messagePort = messagePort;
        this.fileSharePort = fileSharePort;
        this.lastSeen = System.currentTimeMillis();
    }

    public void refresh() {
        this.lastSeen = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public String getHost() {
        return host;
    }

    public int getMessagePort() {
        return messagePort;
    }

    public int getFileSharePort() {
        return fileSharePort;
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
