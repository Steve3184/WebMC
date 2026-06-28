package top.steve3184.webmc.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;

/**
 * Multiplayer manager for WebMC browser client.
 * Handles WebSocket connections to multiplayer servers, player sync, and server discovery.
 */
public final class MultiplayerManager {

    /** Singleton instance */
    public static final MultiplayerManager INSTANCE = new MultiplayerManager();

    /** Current connection state */
    private ConnectionState state = ConnectionState.DISCONNECTED;

    /** Active WebSocket connection */
    private WebSocketClient client;

    /** Server address we're connected to */
    private String currentServerAddress;

    /** Current player info */
    private String playerName;
    private UUID playerId;

    /** Message listeners */
    private final List<Consumer<ServerMessage>> messageListeners = new CopyOnWriteArrayList<>();

    /** Player list listeners */
    private final List<Consumer<List<PlayerInfo>>> playerListListeners = new CopyOnWriteArrayList<>();

    /** Connection state listeners */
    private final List<Consumer<ConnectionState>> stateListeners = new CopyOnWriteArrayList<>();

    /** Connected players */
    private final List<PlayerInfo> connectedPlayers = new CopyOnWriteArrayList<>();

    /** Message types */
    public static final String MSG_TYPE_POSITION = "position";
    public static final String MSG_TYPE_PLAYER_JOIN = "player_join";
    public static final String MSG_TYPE_PLAYER_LEAVE = "player_leave";
    public static final String MSG_TYPE_CHAT = "chat";
    public static final String MSG_TYPE_SERVER_INFO = "server_info";
    public static final String MSG_TYPE_PING = "ping";
    public static final String MSG_TYPE_PONG = "pong";

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        AUTHENTICATING,
        PLAYING,
        ERROR
    }

    /** Player information */
    public static class PlayerInfo {
        public final UUID id;
        public final String name;
        public double x, y, z;
        public float yaw, pitch;
        public boolean onGround;

        public PlayerInfo(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        public PlayerInfo(UUID id, String name, double x, double y, double z, float yaw, float pitch, boolean onGround) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
        }
    }

    /** Server message */
    public static class ServerMessage {
        public final String type;
        public final String sender;
        public final String content;
        public final long timestamp;

        public ServerMessage(String type, String sender, String content) {
            this.type = type;
            this.sender = sender;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private MultiplayerManager() {
        this.playerName = "Player";
        this.playerId = UUID.randomUUID();
    }

    /**
     * Set player identity.
     */
    public void setPlayerIdentity(String name, UUID id) {
        this.playerName = name;
        this.playerId = id;
    }

    /**
     * Connect to a server directly.
     */
    public void connectToServer(String address) {
        connectToServer(address, null);
    }

    /**
     * Connect to a server with optional WebSocket URL override.
     */
    public void connectToServer(String address, String wsUrlOverride) {
        if (state == ConnectionState.CONNECTING || state == ConnectionState.CONNECTED) {
            disconnect();
        }

        this.currentServerAddress = address;
        setState(ConnectionState.CONNECTING);

        // Convert address to WebSocket URL
        String wsUrl;
        if (wsUrlOverride != null && !wsUrlOverride.isEmpty()) {
            wsUrl = wsUrlOverride;
        } else {
            // Determine protocol
            String protocol = address.startsWith("wss://") ? "wss://" : "ws://";
            // Remove protocol if present
            String host = address.replaceFirst("^wss?://", "");
            wsUrl = protocol + host + "/webmc";
        }

        log("Connecting to WebSocket: " + wsUrl);

        client = new WebSocketClient(wsUrl, new WebSocketClient.WebSocketHandler() {
            @Override
            public void onOpen() {
                log("WebSocket connected");
                setState(ConnectionState.CONNECTED);
                // Send join message
                sendJson(buildJoinMessage());
            }

            @Override
            public void onMessage(String data) {
                handleMessage(data);
            }

            @Override
            public void onError(String error) {
                log("WebSocket error: " + error);
                setState(ConnectionState.ERROR);
            }

            @Override
            public void onClose(int code, String reason) {
                log("WebSocket closed: " + code + " - " + reason);
                connectedPlayers.clear();
                setState(ConnectionState.DISCONNECTED);
            }
        });

        client.connect();
    }

    /**
     * Disconnect from current server.
     */
    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
        }
        connectedPlayers.clear();
        setState(ConnectionState.DISCONNECTED);
    }

    /**
     * Send a chat message.
     */
    public void sendChatMessage(String message) {
        if (state != ConnectionState.CONNECTED && state != ConnectionState.PLAYING) {
            return;
        }

        String json = "{" +
            "\"type\":\"" + MSG_TYPE_CHAT + "\"," +
            "\"sender\":\"" + escapeJson(playerName) + "\"," +
            "\"content\":\"" + escapeJson(message) + "\"," +
            "\"timestamp\":" + System.currentTimeMillis() +
            "}";
        sendJson(json);
    }

    /**
     * Send position update.
     */
    public void sendPosition(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        if (state != ConnectionState.CONNECTED && state != ConnectionState.PLAYING) {
            return;
        }

        String json = "{" +
            "\"type\":\"" + MSG_TYPE_POSITION + "\"," +
            "\"playerId\":\"" + playerId.toString() + "\"," +
            "\"x\":" + x + "," +
            "\"y\":" + y + "," +
            "\"z\":" + z + "," +
            "\"yaw\":" + yaw + "," +
            "\"pitch\":" + pitch + "," +
            "\"onGround\":" + onGround +
            "}";
        sendJson(json);
    }

    /**
     * Register a message listener.
     */
    public void addMessageListener(Consumer<ServerMessage> listener) {
        messageListeners.add(listener);
    }

    /**
     * Remove a message listener.
     */
    public void removeMessageListener(Consumer<ServerMessage> listener) {
        messageListeners.remove(listener);
    }

    /**
     * Register a player list listener.
     */
    public void addPlayerListListener(Consumer<List<PlayerInfo>> listener) {
        playerListListeners.add(listener);
    }

    /**
     * Remove a player list listener.
     */
    public void removePlayerListListener(Consumer<List<PlayerInfo>> listener) {
        playerListListeners.remove(listener);
    }

    /**
     * Register a connection state listener.
     */
    public void addStateListener(Consumer<ConnectionState> listener) {
        stateListeners.add(listener);
    }

    /**
     * Remove a connection state listener.
     */
    public void removeStateListener(Consumer<ConnectionState> listener) {
        stateListeners.remove(listener);
    }

    /**
     * Get current connection state.
     */
    public ConnectionState getState() {
        return state;
    }

    /**
     * Get connected players.
     */
    public List<PlayerInfo> getConnectedPlayers() {
        return new ArrayList<>(connectedPlayers);
    }

    /**
     * Get current server address.
     */
    public String getCurrentServerAddress() {
        return currentServerAddress;
    }

    private void handleMessage(String data) {
        // Parse JSON message
        // Expected format: {"type":"...", ...}
        try {
            String type = extractJsonString(data, "type");
            if (type == null) {
                log("Unknown message format: " + data);
                return;
            }

            switch (type) {
                case MSG_TYPE_POSITION:
                    handlePositionMessage(data);
                    break;
                case MSG_TYPE_PLAYER_JOIN:
                    handlePlayerJoinMessage(data);
                    break;
                case MSG_TYPE_PLAYER_LEAVE:
                    handlePlayerLeaveMessage(data);
                    break;
                case MSG_TYPE_CHAT:
                    handleChatMessage(data);
                    break;
                case MSG_TYPE_SERVER_INFO:
                    handleServerInfoMessage(data);
                    break;
                case MSG_TYPE_PLAYER_LIST:
                    handlePlayerListMessage(data);
                    break;
                case MSG_TYPE_PONG:
                    // Handle pong response
                    break;
                default:
                    log("Unknown message type: " + type);
            }
        } catch (Exception e) {
            log("Error parsing message: " + e.getMessage());
        }
    }

    private void handlePositionMessage(String data) {
        String playerIdStr = extractJsonString(data, "playerId");
        if (playerIdStr == null) return;

        UUID id = UUID.fromString(playerIdStr);
        double x = extractJsonDouble(data, "x");
        double y = extractJsonDouble(data, "y");
        double z = extractJsonDouble(data, "z");
        float yaw = (float) extractJsonDouble(data, "yaw");
        float pitch = (float) extractJsonDouble(data, "pitch");
        boolean onGround = extractJsonBoolean(data, "onGround");

        // Update or add player
        PlayerInfo player = findPlayer(id);
        if (player != null) {
            player.x = x;
            player.y = y;
            player.z = z;
            player.yaw = yaw;
            player.pitch = pitch;
            player.onGround = onGround;
        }

        notifyPlayerListChanged();
    }

    private void handlePlayerJoinMessage(String data) {
        String playerIdStr = extractJsonString(data, "playerId");
        String name = extractJsonString(data, "name");
        if (playerIdStr == null || name == null) return;

        UUID id = UUID.fromString(playerIdStr);
        PlayerInfo player = new PlayerInfo(id, name);
        connectedPlayers.add(player);

        notifyPlayerListChanged();
        notifyMessageReceived(new ServerMessage(MSG_TYPE_PLAYER_JOIN, name, name + " joined the game"));
    }

    private void handlePlayerLeaveMessage(String data) {
        String playerIdStr = extractJsonString(data, "playerId");
        if (playerIdStr == null) return;

        UUID id = UUID.fromString(playerIdStr);
        PlayerInfo player = findPlayer(id);
        if (player != null) {
            String name = player.name;
            connectedPlayers.remove(player);
            notifyPlayerListChanged();
            notifyMessageReceived(new ServerMessage(MSG_TYPE_PLAYER_LEAVE, name, name + " left the game"));
        }
    }

    private void handleChatMessage(String data) {
        String sender = extractJsonString(data, "sender");
        String content = extractJsonString(data, "content");
        if (sender == null || content == null) return;

        notifyMessageReceived(new ServerMessage(MSG_TYPE_CHAT, sender, content));
    }

    private void handleServerInfoMessage(String data) {
        String motd = extractJsonString(data, "motd");
        String version = extractJsonString(data, "version");
        int playerCount = (int) extractJsonDouble(data, "playerCount");
        int maxPlayers = (int) extractJsonDouble(data, "maxPlayers");

        // Server info received - could trigger UI update
        setState(ConnectionState.PLAYING);
    }

    private void handlePlayerListMessage(String data) {
        // Parse player list array - simplified implementation
        connectedPlayers.clear();
        // In a real implementation, parse the array of players
        notifyPlayerListChanged();
    }

    private PlayerInfo findPlayer(UUID id) {
        for (PlayerInfo player : connectedPlayers) {
            if (player.id.equals(id)) {
                return player;
            }
        }
        return null;
    }

    private void sendJson(String json) {
        if (client != null && client.isConnected()) {
            client.send(json);
        }
    }

    private String buildJoinMessage() {
        return "{" +
            "\"type\":\"" + MSG_TYPE_PLAYER_JOIN + "\"," +
            "\"playerId\":\"" + playerId.toString() + "\"," +
            "\"name\":\"" + escapeJson(playerName) + "\"" +
            "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractJsonString(String json, String key) {
        // Simple JSON extraction - in production use proper JSON library
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start < 0) return null;
            start += search.length();
            // Check for null
            if (json.substring(start).trim().startsWith("null")) return null;
            // Try to extract string
            if (json.charAt(start) != '"') return null;
            start++;
        } else {
            start += search.length();
        }

        int end = start;
        boolean escaped = false;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            }
            end++;
        }
        return json.substring(start, end);
    }

    private double extractJsonDouble(String json, String key) {
        String value = extractJsonValue(json, key);
        if (value == null) return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean extractJsonBoolean(String json, String key) {
        String value = extractJsonValue(json, key);
        return "true".equalsIgnoreCase(value);
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();

        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length()) return null;

        // String value
        if (json.charAt(start) == '"') {
            return extractJsonString(json, key);
        }

        // Find end of value (comma, }, ], or end)
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']') {
                break;
            }
            end++;
        }
        return json.substring(start, end).trim();
    }

    private void setState(ConnectionState newState) {
        if (state != newState) {
            state = newState;
            for (Consumer<ConnectionState> listener : stateListeners) {
                try {
                    listener.accept(state);
                } catch (Exception e) {
                    log("State listener error: " + e.getMessage());
                }
            }
        }
    }

    private void notifyMessageReceived(ServerMessage message) {
        for (Consumer<ServerMessage> listener : messageListeners) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                log("Message listener error: " + e.getMessage());
            }
        }
    }

    private void notifyPlayerListChanged() {
        List<PlayerInfo> players = new ArrayList<>(connectedPlayers);
        for (Consumer<List<PlayerInfo>> listener : playerListListeners) {
            try {
                listener.accept(players);
            } catch (Exception e) {
                log("Player list listener error: " + e.getMessage());
            }
        }
    }

    private void log(String message) {
        System.out.println("[WebMC/Multiplayer] " + message);
    }
}
