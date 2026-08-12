/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.service;

import com.peerdrop.desktop.state.AppContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileShareServiceTest {

    @Mock
    private AppContext appContext;

    private FileShareService service;
    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        service = FileShareService.create(appContext);
        tempFile = Files.createTempFile("fileshare-test", ".txt");
        Files.writeString(tempFile, "hello peerdrop");
    }

    @AfterEach
    void tearDown() throws IOException {
        service.close();
        Files.deleteIfExists(tempFile);
    }

    @Test
    void hostFile_beforeServiceStarted_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> service.hostFile(tempFile.toFile()));
    }

    @Test
    void hostFile_withInvalidFile_throwsIllegalArgumentException() {
        service.start();

        assertThrows(IllegalArgumentException.class, () -> service.hostFile(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.hostFile(new File("does/not/exist.txt")));
    }

    @Test
    void hostFile_withValidFile_registersFileAndReturnsMetadata() {
        service.start();

        FileShareService.FileMetadata metadata = service.hostFile(tempFile.toFile());

        assertNotNull(metadata.fileId());
        assertEquals(tempFile.toFile().getName(), metadata.fileName());
        assertEquals(tempFile.toFile().length(), metadata.fileSize());
        assertTrue(service.getHostedFiles().containsKey(metadata.fileId()));
    }

    @Test
    void revokeFile_removesFileFromHostedFiles() {
        service.start();
        FileShareService.FileMetadata metadata = service.hostFile(tempFile.toFile());

        service.revokeFile(metadata.fileId());

        assertFalse(service.getHostedFiles().containsKey(metadata.fileId()));
    }

    @Test
    void downloadHandler_servesHostedFile_andAutoRevokesAfterOneDownload() throws Exception {
        service.start();
        FileShareService.FileMetadata metadata = service.hostFile(tempFile.toFile());
        int boundPort = capturedFileSharePort();

        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + boundPort + "/download?id=" + metadata.fileId()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals("hello peerdrop", response.body());

        }
        // The handler revokes a file as soon as it's been served once.
        assertFalse(service.getHostedFiles().containsKey(metadata.fileId()));
    }

    @Test
    void downloadHandler_withUnknownId_returns404() throws Exception {
        service.start();
        int boundPort = capturedFileSharePort();

        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + boundPort + "/download?id=does-not-exist"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(404, response.statusCode());
        }
    }

    @Test
    void close_clearsHostedFiles() {
        service.start();
        service.hostFile(tempFile.toFile());

        service.close();

        assertTrue(service.getHostedFiles().isEmpty());
    }

    /**
     * FileShareService reports the ephemeral port its HttpServer bound to back to
     * AppContext.setFileSharePort(int) during start(). Capturing that call is the
     * simplest way to discover the real port without reaching into private fields.
     */
    private int capturedFileSharePort() {
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(appContext, atLeastOnce()).setFileSharePort(captor.capture());
        return captor.getValue();
    }
}

