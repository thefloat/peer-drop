/*
 * Copyright (c) 2026 Opolopo Eniyan
 * Distributed under the MIT software license, see the accompanying
 * file LICENSE or http://www.opensource.org/licenses/mit-license.php.
 */

package com.peerdrop.desktop.service;

import com.peerdrop.desktop.model.Message;
import com.peerdrop.desktop.model.Peer;
import com.peerdrop.desktop.protocol.JsonCodec;
import com.peerdrop.desktop.state.AppContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private AppContext appContext;

    @Test
    void send_deliversLengthPrefixedJsonMessage_toListeningPeer() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Peer recipient = new Peer("bob", "localhost", serverSocket.getLocalPort(), 0);
            Message original = new Message(Message.MessageType.CHAT, "alice", "hello world");

            CompletableFuture<Message> receivedFuture = CompletableFuture.supplyAsync(() -> {
                try (Socket serverSide = serverSocket.accept();
                     DataInputStream in = new DataInputStream(serverSide.getInputStream())) {
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    return JsonCodec.decode(new String(bytes), Message.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            boolean result = MessageService.send(recipient, original);
            Message received = receivedFuture.get();

            assertTrue(result);
            assertEquals(original.type(), received.type());
            assertEquals(original.sender(), received.sender());
            assertEquals(original.content(), received.content());
        }
    }

    @Test
    void send_returnsFalse_whenPeerIsUnreachable() throws Exception {
        int freePort;
        try (ServerSocket temp = new ServerSocket(0)) {
            freePort = temp.getLocalPort();
        } // socket is closed immediately, so nothing is listening on this port anymore

        Peer recipient = new Peer("bob", "localhost", freePort, 0);

        boolean result = MessageService.send(recipient, new Message(Message.MessageType.CHAT, "alice", "hi"));

        assertFalse(result);
    }

    @Test
    void close_beforeStart_doesNotThrow() {
        MessageService service = MessageService.create(appContext);

        assertDoesNotThrow(service::close);
    }

    @Test
    void nodeConnectionHandler_dropsDisallowedMessageTypes() throws Exception {
        MessageService service = MessageService.create(appContext);
        List<Message> received = new ArrayList<>();
        service.subscribe(received::add);

        deliverToHandler(service, new Message(Message.MessageType.DISCOVERY, "bob", "{}"));

        assertTrue(received.isEmpty(), "DISCOVERY messages should never reach subscribers");
    }

    @Test
    void nodeConnectionHandler_deliversAllowedMessageTypesToSubscribers() throws Exception {
        MessageService service = MessageService.create(appContext);
        List<Message> received = new ArrayList<>();
        service.subscribe(received::add);

        Message chat = new Message(Message.MessageType.CHAT, "bob", "hey there");
        deliverToHandler(service, chat);

        assertEquals(1, received.size());
        assertEquals(chat.content(), received.getFirst().content());
    }

    @Test
    void unsubscribe_stopsFurtherDelivery() throws Exception {
        MessageService service = MessageService.create(appContext);
        List<Message> received = new ArrayList<>();
        Consumer<Message> listener = received::add;
        service.subscribe(listener);
        service.unsubscribe(listener);

        deliverToHandler(service, new Message(Message.MessageType.CHAT, "bob", "hi again"));

        assertTrue(received.isEmpty(), "unsubscribed listener should not receive further messages");
    }

    /**
     * Feeds a single message directly into a MessageService.NodeConnectionHandler,
     * bypassing the real accept loop / thread pool so the handler's filtering logic
     * can be exercised synchronously and deterministically.
     */
    private void deliverToHandler(MessageService service, Message message) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0);
             Socket client = new Socket("localhost", serverSocket.getLocalPort());
             Socket serverSide = serverSocket.accept()) {

            byte[] bytes = JsonCodec.encode(message).getBytes();
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();

            service.new NodeConnectionHandler(serverSide).run();
        }
    }
}

