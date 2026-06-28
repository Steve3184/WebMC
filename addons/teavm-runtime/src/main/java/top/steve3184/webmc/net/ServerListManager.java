package top.steve3184.webmc.net;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages the list of known multiplayer servers for WebMC.
 * Provides server discovery, favorites, and server status ping.
 */
public final class ServerListManager {

    /** Singleton instance */
    public static final ServerListManager INSTANCE = new ServerListManager();

    /** Known servers */
    private final List<ServerEntry> servers = new CopyOnWriteArrayList<>();

    /** Server list listeners */
    private final List<Consumer<List<ServerEntry>>> listeners = new CopyOnWriteArrayList<>();

    /** Loading state */
    private boolean loading = false;

    /** Default server entries (example servers) */
    private static final ServerEntry[] DEFAULT_SERVERS = {
        new ServerEntry("localhost", "localhost:8080", "WebMC Demo Server"),
        new ServerEntry("webmc-demo", "ws://localhost:8080/webmc", "WebMC Local Demo"),
    };

    public ServerListManager() {
        // Initialize with default servers
        for (ServerEntry entry : DEFAULT_SERVERS) {
            servers.add(entry);
        }
    }

    /**
     * Server entry representing a known server.
     */
    public static class ServerEntry {
        public String id;
        public String address;
        public String name;
        public String motd;
        public int playerCount;
        public int maxPlayers;
        public long ping;
        public ServerStatus status;
        public long lastPing;

        public ServerEntry(String id, String address, String name) {
            this.id = id;
            this.address = address;
            this.name = name;
            this.motd = "";
            this.playerCount = 0;
            this.maxPlayers = 0;
            this.ping = -1;
            this.status = ServerStatus.UNKNOWN;
            this.lastPing = 0;
        }

        public ServerEntry(String id, String address, String name, String motd, int playerCount, int maxPlayers) {
            this.id = id;
            this.address = address;
            this.name = name;
            this.motd = motd;
            this.playerCount = playerCount;
            this.maxPlayers = maxPlayers;
            this.ping = -1;
            this.status = ServerStatus.UNKNOWN;
            this.lastPing = System.currentTimeMillis();
        }
    }

    public enum ServerStatus {
        UNKNOWN,
        PENDING,
        ONLINE,
        OFFLINE,
        INCOMPATIBLE
    }

    /**
     * Add a server to the list.
     */
    public void addServer(String id, String address, String name) {
        ServerEntry entry = new ServerEntry(id, address, name);
        servers.add(entry);
        notifyListeners();
    }

    /**
     * Remove a server from the list.
     */
    public void removeServer(String id) {
        servers.removeIf(entry -> entry.id.equals(id));
        notifyListeners();
    }

    /**
     * Update server information.
     */
    public void updateServer(String id, String name, String motd, int playerCount, int maxPlayers, long ping) {
        for (ServerEntry entry : servers) {
            if (entry.id.equals(id)) {
                entry.name = name;
                entry.motd = motd;
                entry.playerCount = playerCount;
                entry.maxPlayers = maxPlayers;
                entry.ping = ping;
                entry.status = ServerStatus.ONLINE;
                entry.lastPing = System.currentTimeMillis();
                break;
            }
        }
        notifyListeners();
    }

    /**
     * Get all servers.
     */
    public List<ServerEntry> getServers() {
        return new ArrayList<>(servers);
    }

    /**
     * Find a server by address.
     */
    public ServerEntry findByAddress(String address) {
        for (ServerEntry entry : servers) {
            if (entry.address.equalsIgnoreCase(address)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Find a server by ID.
     */
    public ServerEntry findById(String id) {
        for (ServerEntry entry : servers) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Register a listener for server list changes.
     */
    public void addListener(Consumer<List<ServerEntry>> listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener.
     */
    public void removeListener(Consumer<List<ServerEntry>> listener) {
        listeners.remove(listener);
    }

    /**
     * Refresh all servers by pinging them.
     */
    public void refreshServers() {
        if (loading) return;
        loading = true;

        // Mark all as pending
        for (ServerEntry entry : servers) {
            entry.status = ServerStatus.PENDING;
        }

        notifyListeners();

        // In a real implementation, this would ping each server
        // For now, simulate with localhost
        new Thread(() -> {
            try {
                // Simulate ping delay
                Thread.sleep(500);

                // Update localhost as online for demo
                ServerEntry local = findByAddress("localhost:8080");
                if (local != null) {
                    local.status = ServerStatus.ONLINE;
                    local.ping = 10;
                    local.playerCount = 0;
                    local.maxPlayers = 10;
                }

                loading = false;
                notifyListeners();
            } catch (InterruptedException e) {
                loading = false;
            }
        }).start();
    }

    /**
     * Clear all servers.
     */
    public void clearAll() {
        servers.clear();
        notifyListeners();
    }

    /**
     * Reset to default servers.
     */
    public void resetToDefaults() {
        servers.clear();
        for (ServerEntry entry : DEFAULT_SERVERS) {
            servers.add(entry);
        }
        notifyListeners();
    }

    private void notifyListeners() {
        List<ServerEntry> currentServers = new ArrayList<>(servers);
        for (Consumer<List<ServerEntry>> listener : listeners) {
            try {
                listener.accept(currentServers);
            } catch (Exception e) {
                System.out.println("[WebMC/ServerList] Listener error: " + e.getMessage());
            }
        }
    }
}
