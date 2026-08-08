/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.model;

import java.io.File;
import java.util.UUID;

public record FileMetadata(String fileId, String fileName, long fileSize) {
    public FileMetadata(File file) {
        this(UUID.randomUUID().toString(), file.getName(), file.length());
    }
}
