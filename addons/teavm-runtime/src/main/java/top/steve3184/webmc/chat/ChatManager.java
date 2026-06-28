package top.steve3184.webmc.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Chat manager for WebMC browser client.
 * Handles chat message display, history, and system messages.
 */
public final class ChatManager {

    /** Singleton instance */
    public static final ChatManager INSTANCE = new ChatManager();

    /** Chat history */
    private final List<ChatMessage> chatHistory = new CopyOnWriteArrayList<>();

    /** Chat message listeners */
    private final List<Consumer<ChatMessage>> messageListeners = new CopyOnWriteArrayList<>();

    /** Maximum chat history size */
    private static final int MAX_HISTORY_SIZE = 100;

    /** Maximum visible messages */
    private static final int MAX_VISIBLE_MESSAGES = 8;

    /** Current player info */
    private String playerName = "Player";
    private UUID playerId;

    /** Chat visibility state */
    private boolean chatVisible = false;
    private boolean inputVisible = false;

    private ChatManager() {
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
     * Get player name.
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Get player ID.
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Add a chat message to display.
     */
    public void addMessage(String sender, String content, ChatMessageType type) {
        if (content == null || content.isEmpty()) {
            return;
        }

        ChatMessage message = new ChatMessage(
            sender != null ? sender : "",
            content,
            type != null ? type : ChatMessageType.CHAT,
            System.currentTimeMillis()
        );

        chatHistory.add(message);

        // Trim old messages if needed
        while (chatHistory.size() > MAX_HISTORY_SIZE) {
            chatHistory.remove(0);
        }

        // Notify listeners
        notifyMessageReceived(message);

        // Forward to web UI
        forwardToWebUI(message);
    }

    /**
     * Add a player chat message.
     */
    public void addPlayerMessage(String sender, String content) {
        addMessage(sender, content, ChatMessageType.CHAT);
    }

    /**
     * Add a system message.
     */
    public void addSystemMessage(String content) {
        addMessage(null, content, ChatMessageType.SYSTEM);
    }

    /**
     * Add an info message.
     */
    public void addInfoMessage(String content) {
        addMessage(null, content, ChatMessageType.INFO);
    }

    /**
     * Add a warning message.
     */
    public void addWarningMessage(String content) {
        addMessage(null, content, ChatMessageType.WARNING);
    }

    /**
     * Add an error message.
     */
    public void addErrorMessage(String content) {
        addMessage(null, content, ChatMessageType.ERROR);
    }

    /**
     * Add a player join message.
     */
    public void addPlayerJoinMessage(String playerName) {
        addMessage(null, playerName + " joined the game", ChatMessageType.INFO);
    }

    /**
     * Add a player leave message.
     */
    public void addPlayerLeaveMessage(String playerName) {
        addMessage(null, playerName + " left the game", ChatMessageType.INFO);
    }

    /**
     * Add a sent message (echo back to sender).
     */
    public void addSentMessage(String content) {
        addMessage(playerName, content, ChatMessageType.SENT);
    }

    /**
     * Get recent messages for display.
     */
    public List<ChatMessage> getRecentMessages(int count) {
        int size = Math.min(count, chatHistory.size());
        List<ChatMessage> recent = new ArrayList<>();
        for (int i = chatHistory.size() - size; i < chatHistory.size(); i++) {
            recent.add(chatHistory.get(i));
        }
        return recent;
    }

    /**
     * Get all chat history.
     */
    public List<ChatMessage> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    /**
     * Get chat history for display (limited).
     */
    public List<ChatMessage> getDisplayMessages() {
        return getRecentMessages(MAX_VISIBLE_MESSAGES);
    }

    /**
     * Clear chat history.
     */
    public void clearHistory() {
        chatHistory.clear();
    }

    /**
     * Register a message listener.
     */
    public void addMessageListener(Consumer<ChatMessage> listener) {
        messageListeners.add(listener);
    }

    /**
     * Remove a message listener.
     */
    public void removeMessageListener(Consumer<ChatMessage> listener) {
        messageListeners.remove(listener);
    }

    /**
     * Set chat visibility state.
     */
    public void setChatVisible(boolean visible) {
        this.chatVisible = visible;
    }

    /**
     * Check if chat is visible.
     */
    public boolean isChatVisible() {
        return chatVisible;
    }

    /**
     * Set chat input visibility.
     */
    public void setInputVisible(boolean visible) {
        this.inputVisible = visible;
    }

    /**
     * Check if chat input is visible.
     */
    public boolean isInputVisible() {
        return inputVisible;
    }

    /**
     * Handle outgoing chat message from UI.
     * Called when user sends a message through the chat input.
     */
    public void handleOutgoingMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        String trimmed = message.trim();

        // Check if it's a command
        if (trimmed.startsWith("/")) {
            handleCommand(trimmed);
        } else {
            // Regular chat message - will be sent via MultiplayerManager
            notifyOutgoingChat(trimmed);
        }
    }

    /**
     * Handle a chat command.
     */
    private void handleCommand(String command) {
        log("[Chat] Command: " + command);
        // Commands are handled by the game engine
        // Notify listeners that a command was issued
    }

    /**
     * Forward message to web UI.
     */
    private void forwardToWebUI(ChatMessage message) {
        try {
            // Call the JavaScript function to display the message
            String escapedContent = escapeForJavaScript(message.content);
            String escapedSender = escapeForJavaScript(message.sender);
            String typeStr = message.type.name().toLowerCase();

            String script = String.format(
                "(function(){ if (typeof window.__webmcAddChatMessage === 'function') { " +
                "window.__webmcAddChatMessage('%s', '%s', '%s'); } })();",
                escapedContent, typeStr, escapedSender
            );

            evalJavaScript(script);
        } catch (Exception e) {
            log("Error forwarding to web UI: " + e.getMessage());
        }
    }

    /**
     * Notify listeners of incoming message.
     */
    private void notifyMessageReceived(ChatMessage message) {
        for (Consumer<ChatMessage> listener : messageListeners) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                log("Message listener error: " + e.getMessage());
            }
        }
    }

    /**
     * Notify listeners of outgoing chat message.
     */
    private final List<Consumer<String>> outgoingChatListeners = new CopyOnWriteArrayList<>();

    public void addOutgoingChatListener(Consumer<String> listener) {
        outgoingChatListeners.add(listener);
    }

    public void removeOutgoingChatListener(Consumer<String> listener) {
        outgoingChatListeners.remove(listener);
    }

    private void notifyOutgoingChat(String message) {
        for (Consumer<String> listener : outgoingChatListeners) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                log("Outgoing chat listener error: " + e.getMessage());
            }
        }
    }

    /**
     * Escape string for JavaScript.
     */
    private String escapeForJavaScript(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\"", "\\\"");
    }

    /**
     * Evaluate JavaScript.
     */
    private native void evalJavaScript(String script);

    /**
     * Chat message type.
     */
    public enum ChatMessageType {
        /** Player chat message */
        CHAT,
        /** System message */
        SYSTEM,
        /** Information message */
        INFO,
        /** Warning message */
        WARNING,
        /** Error message */
        ERROR,
        /** Message sent by local player */
        SENT
    }

    /**
     * Chat message data class.
     */
    public static class ChatMessage {
        public final String sender;
        public final String content;
        public final ChatMessageType type;
        public final long timestamp;

        public ChatMessage(String sender, String content, ChatMessageType type, long timestamp) {
            this.sender = sender;
            this.content = content;
            this.type = type;
            this.timestamp = timestamp;
        }

        /**
         * Check if this is a player message.
         */
        public boolean isPlayerMessage() {
            return sender != null && !sender.isEmpty();
        }

        /**
         * Check if this is a system message.
         */
        public boolean isSystemMessage() {
            return type == ChatMessageType.SYSTEM ||
                   type == ChatMessageType.INFO ||
                   type == ChatMessageType.WARNING ||
                   type == ChatMessageType.ERROR;
        }

        /**
         * Get formatted display string.
         */
        public String getDisplayText() {
            if (isPlayerMessage()) {
                return "<" + sender + "> " + content;
            }
            return content;
        }
    }

    private void log(String message) {
        System.out.println("[WebMC/Chat] " + message);
    }
}