package com.peerdrop.desktop.service;

import com.peerdrop.desktop.model.Message;
import com.peerdrop.desktop.model.Peer;
import com.peerdrop.desktop.protocol.JsonCodec;
import com.peerdrop.desktop.state.AppContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MessageService {
    private static final int SHUTDOWN_TIMEOUT_MS = 10000;
    private final CopyOnWriteArrayList<Consumer<Message>> listeners =
            new CopyOnWriteArrayList<>();
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final AppContext appContext;

    private ServerSocket serverSocket;
    private ExecutorService peerConnPool;
    private volatile boolean isRunning = false;

    private MessageService(AppContext appContext) {
        this.appContext = appContext;
    }

    private void init() throws IOException {
        serverSocket = new ServerSocket(0);
        peerConnPool = Executors.newCachedThreadPool(r -> {
            var t = new Thread(r);
            t.setName("MsgService-PeerConn-Worker-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    public static MessageService create(AppContext appContext) {
        return new MessageService(appContext);
    }

    public static boolean send(
            Peer recipient, Message message
    ) {
        try (
                Socket socket =
                        new Socket(
                                recipient.getHost(),
                                recipient.getMessagePort()
                        );

                DataOutputStream out =
                        new DataOutputStream(
                                socket.getOutputStream()
                        )
        ) {

            var json = JsonCodec.encode(message);
            byte[] bytes = json.getBytes();

            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();

        } catch (UnknownHostException e) {
            System.err.println("[Msg Service] Couldn't determine IP address.");
            return false;
        } catch (IOException e) {
            System.err.println("[Msg Service] I/O error while creating socket");
            return false;
        }
        return true;
    }

    private void notifyListeners(Message message) {
        for (Consumer<Message> listener : listeners) {
            listener.accept(message);
        }
    }

    private void acceptLoop() {
        try {
            appContext.setMessagePort(serverSocket.getLocalPort());

            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    peerConnPool.execute(new NodeConnectionHandler(socket));

                } catch (SocketException e) {
                    if (isRunning) {
                        System.err.println("[Msg Service] Unexpected socket error in TCP accept loop.");
                        break;
                    } else {
                        System.out.println("[Msg Service] TCP accept socket closed.");
                    }
                } catch (IOException e) {
                    System.err.println(
                            "[Msg Service] Error in TCP accept loop while waiting for connection: \n" +
                                    "Socket closed or not bound");
                    break;
                }
            }
        } finally {
            close();
        }
    }

    public void start() {
        if (isRunning) { return; }

        isRunning = true;

        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException("[Msg Service] I/O error while initializing MessageService." + e);
        }

        Thread acceptThread = new Thread(this::acceptLoop, "MsgService-Accept-Loop");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void close() {
        if (!isRunning) return;
        isRunning = false;

        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("[Msg Service] Error while closing server socket");
        }

        peerConnPool.shutdown();
        try {
            if (!peerConnPool.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                peerConnPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Pool Interrupted while awaiting completion of tasks.");
        }
    }

    public void subscribe(Consumer<Message> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<Message> listener) {
        listeners.remove(listener);
    }

    public class NodeConnectionHandler implements Runnable {

        private final Socket socket;

        public NodeConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in =
                         new DataInputStream(socket.getInputStream())
            ) {

                int length = in.readInt();
                byte[] bytes = new byte[length];
                in.readFully(bytes);

                String json = new String(bytes);
                Message message = JsonCodec.decode(json, Message.class);

                if (!List.of(Message.MessageType.CHAT, Message.MessageType.FILE_OFFER).contains(message.type())) {
                    return;
                }

                notifyListeners(message);

            } catch (IOException e) {
                System.err.println("[Msg Service] I/O error while creating input stream.");
            }
        }
    }
}