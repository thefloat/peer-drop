/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

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
