package com.peerdrop.desktop.state;

import com.peerdrop.desktop.model.Peer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {

    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    public void addOrUpdate(Peer peer) {

        Peer existing =
                peers.get(peer.getUsername());

        if (existing != null) {
            existing.refresh();
            return;
        }

        peers.put(peer.getUsername(), peer);
    }

    public Peer get(String username) {
        return peers.get(username);
    }

    public Map<String, Peer> getAll() {
        return peers;
    }

    public void removeInactive(long timeoutMillis) {

        long now = System.currentTimeMillis();

        peers.values().removeIf(
                peer -> now - peer.getLastSeen() > timeoutMillis
        );
    }
}
