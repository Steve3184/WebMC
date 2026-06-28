package top.steve3184.webmc.chat;

import top.steve3184.webmc.net.MultiplayerManager;
import top.steve3184.webmc.net.MultiplayerManager.ServerMessage;

/**
 * Bridge between MultiplayerManager and ChatManager.
 * Handles chat messages from the multiplayer WebSocket connection.
 */
public final class ChatMultiplayerBridge {

    /** Singleton instance */
    public static final ChatMultiplayerBridge INSTANCE = new ChatMultiplayerBridge();

    /** Reference to MultiplayerManager */
    private MultiplayerManager multiplayerManager;

    private ChatMultiplayerBridge() {
        initialize();
    }

    /**
     * Initialize the bridge.
     */
    public void initialize() {
        multiplayerManager = MultiplayerManager.INSTANCE;

        // Register message listener with MultiplayerManager
        multiplayerManager.addMessageListener(this::handleServerMessage);

        System.out.println("[mc-web/chat] Chat multiplayer bridge initialized");
    }

    /**
     * Handle incoming server message.
     */
    private void handleServerMessage(ServerMessage message) {
        if (message == null) {
            return;
        }

        String type = message.type;
        String sender = message.sender;
        String content = message.content;

        switch (type) {
            case MultiplayerManager.MSG_TYPE_CHAT:
                // Player chat message
                if (sender != null && content != null) {
                    ChatManager.INSTANCE.addPlayerMessage(sender, content);
                }
                break;

            case MultiplayerManager.MSG_TYPE_PLAYER_JOIN:
                // Player joined
                if (content != null) {
                    ChatManager.INSTANCE.addPlayerJoinMessage(content);
                }
                break;

            case MultiplayerManager.MSG_TYPE_PLAYER_LEAVE:
                // Player left
                if (content != null) {
                    ChatManager.INSTANCE.addPlayerLeaveMessage(content);
                }
                break;

            case MultiplayerManager.MSG_TYPE_SERVER_INFO:
                // Server info - typically shown as system message
                if (content != null) {
                    ChatManager.INSTANCE.addSystemMessage(content);
                }
                break;

            default:
                // Unknown message type, ignore
                break;
        }
    }

    /**
     * Send a chat message to the server.
     */
    public void sendChatMessage(String message) {
        if (multiplayerManager != null) {
            multiplayerManager.sendChatMessage(message);
        }
    }

    /**
     * Get current connection state.
     */
    public MultiplayerManager.ConnectionState getConnectionState() {
        if (multiplayerManager != null) {
            return multiplayerManager.getState();
        }
        return MultiplayerManager.ConnectionState.DISCONNECTED;
    }
}