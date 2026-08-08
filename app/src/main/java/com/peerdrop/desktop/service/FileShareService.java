package com.peerdrop.desktop.service;

import com.peerdrop.desktop.state.AppContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FileShareService {
    private final AppContext appContext;
    private final ConcurrentHashMap<String, File> hostedFiles = new ConcurrentHashMap<>();
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private HttpServer httpServer;
    private volatile boolean isRunning = false;

    private FileShareService(AppContext appContext) {
        this.appContext = appContext;
    }

    private void init() throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        this.httpServer.createContext("/download", new FileSharingHandler());
        this.httpServer.setExecutor(Executors.newCachedThreadPool(r -> {
            var t = new Thread(r);
            t.setName("FSService-HttpServer-Worker-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }));

        appContext.setFileSharePort(httpServer.getAddress().getPort());
    }

    public static FileShareService create(AppContext appContext) {
        return new FileShareService(appContext);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public ConcurrentHashMap<String, File> getHostedFiles() {
        return hostedFiles;
    }

    /**
     * Prepares a file to be hosted. Generates unique metadata that your View Model
     * can serialize into a JSON/text message and send to the peer via the chat channel.
     */
    public FileMetadata hostFile(File file) {
        if (!isRunning) {
            throw new IllegalStateException("The service isn't running, please call fileService.start() first.");
        }

        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid file provided for hosting.");
        }

        // Generate a random ID so peers can't guess files via brute force directory traversal
        String fileId = UUID.randomUUID().toString();
        hostedFiles.put(fileId, file);
        pcs.firePropertyChange(
                "hostedFiles", null, null);

        return new FileMetadata(fileId, file.getName(), file.length());
    }


    public void revokeFile(String fileId) {
        hostedFiles.remove(fileId);
        pcs.firePropertyChange(
                "hostedFiles", null, null);
    }

    /**
     * Downloads a file from a peer asynchronously. Invokes progress_listener
     * ticks to update UI progress bars smoothly.
     */
    public void downloadFile(String host, int port, FileMetadata fileMetadata,
                             File destinationDir, ProgressListener progressListener) {

        // Run asynchronously so the Chat ViewModel UI thread stays perfectly responsive
        CompletableFuture.runAsync(() -> {
            try (HttpClient client = HttpClient.newHttpClient()) {
                Path destDir = destinationDir.toPath();
                if (!Files.exists(destDir)) {
                    Files.createDirectories(destDir);
                }
                Path targetPath = destDir.resolve(fileMetadata.fileName());

                String encodedId = URLEncoder.encode(fileMetadata.fileId(), StandardCharsets.UTF_8);
                String url = String.format("http://%s:%d/download?id=%s", host, port, encodedId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                // Custom streaming handler to manually step through bytes and track UI progress
                client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                        .thenAccept(response -> {
                            if (response.statusCode() != 200) {
                                progressListener.onError(new IOException("Server error: " + response.statusCode()));
                                return;
                            }

                            try (InputStream is = response.body();
                                 OutputStream os = Files.newOutputStream(targetPath)) {

                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                long totalBytesRead = 0;
                                long totalExpected = fileMetadata.fileSize();

                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                    totalBytesRead += bytesRead;

                                    // Trigger progress update hook
                                    progressListener.onProgress(
                                            (double) totalBytesRead / totalExpected);
                                }
                                progressListener.onComplete(targetPath);
                            } catch (IOException e) {
                                progressListener.onError(e);
                            }
                        }).join();

            } catch (Exception e) {
                progressListener.onError(e);
            }
        });
    }

    public void start() {
        if (isRunning) { return; }

        isRunning = true;

        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException("[FileShare Service] I/O error while initializing FileShareService.\n"+ e);
        }

        this.httpServer.start();
    }

    /**
     * Stops the underlying HTTP server and clears up hosted references.
     */
    public void close() {
        if (!isRunning) return;
        isRunning = false;

        httpServer.stop(0);
        hostedFiles.clear();
        pcs.firePropertyChange(
                "hostedFiles", null, null);
    }

    // Callback interface for your Chat View Model to update UI progress bars
    public interface ProgressListener {
        void onProgress(double fractionDownloaded);

        void onComplete(Path filePath);

        void onError(Exception e);
    }

    // Nested Metadata Record/Class for tracking offered files
    public record FileMetadata(String fileId, String fileName, long fileSize) {
        public FileMetadata(File file) {
            this(UUID.randomUUID().toString(), file.getName(), file.length());
        }
    }

    // === INNER HTTP HANDLER ===
    private class FileSharingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // Extract the secure ?id= uuid parameter
            String query = exchange.getRequestURI().getQuery();
            String fileId = null;
            if (query != null && query.startsWith("id=")) {
                fileId = query.substring(3);
            }

            File file = (fileId != null) ? hostedFiles.get(fileId) : null;
            if (file == null || !file.exists()) {
                byte[] error = "File not found or link expired.".getBytes();
                exchange.sendResponseHeaders(404, error.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error);
                }
                return;
            }

            // Serve the file safely
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(os);
                    revokeFile(fileId);
            }
        }
    }
}