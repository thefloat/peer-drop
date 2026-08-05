package com.peerdrop.desktop.model;

import java.io.File;
import java.util.UUID;

public record FileMetadata(String fileId, String fileName, long fileSize) {
    public FileMetadata(File file) {
        this(UUID.randomUUID().toString(), file.getName(), file.length());
    }
}
