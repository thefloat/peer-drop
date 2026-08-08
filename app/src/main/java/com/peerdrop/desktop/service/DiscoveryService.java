package com.peerdrop.desktop.service;

import com.peerdrop.desktop.model.Message;
import com.peerdrop.desktop.model.Peer;
import com.peerdrop.desktop.model.PeerSession;
import com.peerdrop.desktop.service.util.NetworkInterfaceUtils;
import com.peerdrop.desktop.protocol.JsonCodec;
import com.peerdrop.desktop.state.AppContext;
import com.peerdrop.desktop.state.PeerRegistry;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DiscoveryService {
    private static final String GROUP = "230.0.0.1";
    private static final int DISCOVERY_PORT = 8888;

    private final CopyOnWriteArrayList<Consumer<List<Peer>>> listeners =
            new CopyOnWriteArrayList<>();
    private MulticastSocket multicastSocket;
    private PeerRegistry peerRegistry;
    private final AppContext appContext;

    private volatile boolean isRunning = false;

    private DiscoveryService(AppContext appContext) {
        this.appContext = appContext;
    }

    private void init() throws IOException {
        peerRegistry = new PeerRegistry();
        multicastSocket = new MulticastSocket(DISCOVERY_PORT);

        var selected = appContext.getSelectedNetworkInterface();
        var networkInterface = selected != null
                ? selected
                : selectNetworkInterface();
        multicastSocket.setNetworkInterface(networkInterface);
        // multicastSocket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false); // Disable loop back

    }

    public static DiscoveryService create(AppContext appContext) {
        return new DiscoveryService(appContext);
    }

    private NetworkInterface selectNetworkInterface() throws IOException {
        List<NetworkInterface> viableInterfaces = NetworkInterfaceUtils.getViableInterfaces();

        if (viableInterfaces.isEmpty()) {
            System.err.println(
                    "[Dsc Service] No viable network interfaces found for multicast, defaulting to localhost...");

            return NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        }

        var selectedInterface = viableInterfaces.stream().filter(NetworkInterfaceUtils::isLikelyWireless)
                .findFirst()
                .orElse(viableInterfaces.getFirst());

        appContext.setSelectedNetworkInterface(selectedInterface);

        System.out.println("[Dsc Service] Network Interface: " + selectedInterface.getDisplayName());

        return selectedInterface;
    }

    private PeerSession getPeerSession() {
        return appContext.getPeerSession();
    }

    private void notifyListeners() {
        var currentNodes = List.copyOf(peerRegistry.getAll().values());
        for (Consumer<List<Peer>> listener : listeners) {
            listener.accept(currentNodes);
        }
    }

    private void receiveLoop() {
        try {
            InetSocketAddress group =
                    new InetSocketAddress(InetAddress.getByName(GROUP), DISCOVERY_PORT);
            multicastSocket.joinGroup(group, null);

            byte[] msgBytes = new byte[2048];

            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length);
                    multicastSocket.receive(packet);

                    String json = new String(
                            packet.getData(),
                            0,
                            packet.getLength());

                    Message message = JsonCodec.decode(json, Message.class);

                    if (message.type() != Message.MessageType.DISCOVERY) {
                        continue;
                    }

                    ServicePorts servicePorts =
                            JsonCodec.decode(message.content(), ServicePorts.class);

                    var username = getPeerSession().username();
                    if (username == null || message.sender().equals(username)) {
                        continue;
                    }

                    Peer peer = new Peer(
                            message.sender(),
                            packet.getAddress()
                                    .getHostAddress(),
                            servicePorts.messagePort(),
                            servicePorts.fileSharePort());

                    peerRegistry.addOrUpdate(peer);

                    notifyListeners();
                } catch (SocketException e) {
                    if (isRunning) {
                        System.err.println(
                                "[Dsc Service] Unexpected receiveSocket error in Discovery Service receive loop.");
                        break;
                    } else {
                        System.out.println("[Dsc Service] Discovery Service receive loop receiveSocket closed.");
                    }
                } catch (IOException e) {
                    System.err.println(
                            "[Dsc Service] I/O error in Discovery Service receive loop " +
                                    "while waiting for connection: \n" +
                                    "Socket closed or not bound");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[Dsc Service] Error while joining multicast group");
        } finally {
            close();
        }
    }

    private void broadcastLoop() {
        try {
            while (isRunning) {
                var username = getPeerSession().username();
                if (username == null) {
                    continue;
                }

                var payload = JsonCodec.encode(
                        new ServicePorts(
                                appContext.getMessagePort(),
                                appContext.getFileSharePort()
                        )
                );
                Message message =
                        new Message(
                                Message.MessageType.DISCOVERY,
                                username,
                                payload
                        );

                String json = JsonCodec.encode(message);

                byte[] data = json.getBytes();

                DatagramPacket packet =
                        new DatagramPacket(
                                data,
                                data.length,
                                InetAddress.getByName(GROUP),
                                DISCOVERY_PORT
                        );

                multicastSocket.send(packet);

                Thread.sleep(3000);
            }
        } catch (InterruptedException e) {
            System.err.println("Receive loop thread interrupted.");
        } catch (UnknownHostException e) {
            System.err.println("Unrecognized multicast ip or host.");
        } catch (IOException e) {
            System.err.println("Error while sending datagram packet to multicast group.");
        } finally {
            close();
        }
    }

    private void cleanerLoop() {
        while (isRunning) {
            try {
                peerRegistry.removeInactive(15000);
                notifyListeners();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.err.println("[Dsc Service] Peer registry cleanup loop thread interrupted.");
            }
        }
    }

    private void startThread(Runnable loop, String name) {
        Thread t = new Thread(loop);
        t.setName(name);
        t.setDaemon(true);
        t.start();
    }

    public void subscribe(Consumer<List<Peer>> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<List<Peer>> listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;

        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException("[Dsc Service] I/O error while initializing DiscoveryService.\n" + e);
        }

        startThread(this::receiveLoop, "DscService-Receive-Loop");
        startThread(this::broadcastLoop, "DscService-Broadcast-Loop");
        startThread(this::cleanerLoop, "DscService-Cleanup-Loop");
    }

    public synchronized void close() {
        if (!isRunning) return;
        isRunning = false;

        multicastSocket.close();
    }

    public record ServicePorts(int messagePort, int fileSharePort) {
    }
}
