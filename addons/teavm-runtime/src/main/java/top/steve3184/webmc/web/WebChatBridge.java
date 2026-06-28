package top.steve3184.webmc.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import top.steve3184.webmc.chat.ChatManager;
import top.steve3184.webmc.chat.ChatManager.ChatMessage;
import top.steve3184.webmc.chat.ChatManager.ChatMessageType;
import top.steve3184.webmc.chat.ChatMultiplayerBridge;

/**
 * Bridge between Minecraft's chat system and the web UI.
 *
 * <p>This class connects the Java-side Minecraft chat to the JavaScript
 * chat display in bootstrap.js. It receives chat messages from Minecraft
 * and passes them to the web UI, and receives outgoing chat messages
 * from the web UI and sends them to the server.</p>
 */
public final class WebChatBridge {

    private WebChatBridge() {}

    /**
     * Initialize the chat bridge. Called when the game world is loaded.
     */
    public static void initialize() {
        // Initialize the chat multiplayer bridge
        ChatMultiplayerBridge.INSTANCE.initialize();

        // Register JavaScript callback for outgoing chat messages
        registerJavaScriptBridge();

        // Register listener for outgoing chat
        ChatManager.INSTANCE.addOutgoingChatListener(message -> {
            ChatMultiplayerBridge.INSTANCE.sendChatMessage(message);
        });

        System.out.println("[mc-web/chat] Chat bridge initialized");
    }

    /**
     * Add a chat message to the web display.
     *
     * @param message The message text (may contain formatting codes)
     * @param type Message type: "chat", "system", "info", "warning", "error"
     * @param sender The sender's name, or null for system messages
     */
    public static void addWebChatMessage(String message, String type, String sender) {
        if (message == null || message.isEmpty()) {
            return;
        }

        // Strip Minecraft formatting codes for web display
        String cleanMessage = stripFormatting(message);

        // Parse type
        ChatMessageType messageType = parseMessageType(type);

        // Add to chat manager (which forwards to web UI)
        ChatManager.INSTANCE.addMessage(sender, cleanMessage, messageType);
    }

    /**
     * Add a system message to the web display.
     */
    public static void addSystemMessage(String message) {
        addWebChatMessage(message, "system", null);
    }

    /**
     * Add a player chat message to the web display.
     */
    public static void addPlayerChatMessage(String sender, String message) {
        addWebChatMessage(message, "chat", sender);
    }

    /**
     * Add an info message.
     */
    public static void addInfoMessage(String message) {
        addWebChatMessage(message, "info", null);
    }

    /**
     * Add a warning message.
     */
    public static void addWarningMessage(String message) {
        addWebChatMessage(message, "warning", null);
    }

    /**
     * Add an error message.
     */
    public static void addErrorMessage(String message) {
        addWebChatMessage(message, "error", null);
    }

    /**
     * Add a player join notification.
     */
    public static void addPlayerJoinMessage(String playerName) {
        ChatManager.INSTANCE.addPlayerJoinMessage(playerName);
    }

    /**
     * Add a player leave notification.
     */
    public static void addPlayerLeaveMessage(String playerName) {
        ChatManager.INSTANCE.addPlayerLeaveMessage(playerName);
    }

    /**
     * Parse message type string to ChatMessageType enum.
     */
    private static ChatMessageType parseMessageType(String type) {
        if (type == null) {
            return ChatMessageType.CHAT;
        }

        switch (type.toLowerCase()) {
            case "system":
                return ChatMessageType.SYSTEM;
            case "info":
                return ChatMessageType.INFO;
            case "warning":
                return ChatMessageType.WARNING;
            case "error":
                return ChatMessageType.ERROR;
            case "sent":
                return ChatMessageType.SENT;
            case "chat":
            default:
                return ChatMessageType.CHAT;
        }
    }

    /**
     * Strip Minecraft formatting codes from a message.
     */
    private static String stripFormatting(String message) {
        if (message == null) {
            return "";
        }
        // Remove section sign (§) followed by a formatting code
        return message.replaceAll("§[0-9a-fk-or]", "");
    }

    /**
     * Register the bridge with the window object so JavaScript can call back.
     */
    private static void registerJavaScriptBridge() {
        evalJavaScript(
            "window.__webmcSendChatMessage = function(msg) { " +
            "  try { " +
            "    if (typeof window.__webmcHandleOutgoingChat === 'function') { " +
            "      window.__webmcHandleOutgoingChat(msg); " +
            "    } else { " +
            "      console.warn('[mc-web/chat] Handler not ready for:', msg); " +
            "    } " +
            "  } catch(e) { console.error('[mc-web/chat] Send error:', e.message); } " +
            "};"
        );

        // Also expose the chat bridge for Java side to call JavaScript
        evalJavaScript(
            "window.__webmcChatBridgeReady = true;" +
            "window.__webmcGetChatHistory = function() { " +
            "  return JSON.stringify(window.__webmcChatHistory || []); " +
            "};"
        );
    }

    /**
     * Call a JavaScript function with arguments.
     */
    private static void callJavaScript(String functionName, Object... args) {
        try {
            StringBuilder script = new StringBuilder();
            script.append("(function(){ if (typeof ").append(functionName).append(" === 'function') { ");
            script.append(functionName).append("(");

            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    script.append(", ");
                }
                if (args[i] == null) {
                    script.append("null");
                } else if (args[i] instanceof String) {
                    // Escape special characters for JavaScript string
                    String str = (String) args[i];
                    str = str.replace("\\", "\\\\")
                             .replace("'", "\\'")
                             .replace("\n", "\\n")
                             .replace("\r", "\\r");
                    script.append("'").append(str).append("'");
                } else {
                    script.append("String(").append(args[i]).append(")");
                }
            }

            script.append("); } })();");

            evalJavaScript(script.toString());
        } catch (Exception e) {
            System.err.println("[mc-web/chat] Error calling JS: " + e.getMessage());
        }
    }

    @JSBody(script = "try { eval(arguments[0]); } catch(e) { console.error('[mc-web/chat] JS error:', e.message); }")
    private static native void evalJavaScript(String script);
}