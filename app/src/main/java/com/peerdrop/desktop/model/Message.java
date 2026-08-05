package com.peerdrop.desktop.model;

import java.util.UUID;

public record Message(String id, MessageType type, String sender, String content) {

    public Message(MessageType type, String sender, String content) {
        this(UUID.randomUUID().toString(), type, sender, content);
    }

    public enum MessageType {
        CHAT,
        DISCOVERY,
        FILE_OFFER
    }
}
