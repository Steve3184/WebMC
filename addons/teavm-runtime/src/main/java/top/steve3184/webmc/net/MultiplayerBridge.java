package top.steve3184.webmc.net;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * JavaScript bridge for multiplayer functionality.
 * Provides access to JavaScript-side WebSocket management and event handling.
 */
public final class MultiplayerBridge {

    /** Singleton instance */
    public static final MultiplayerBridge INSTANCE = new MultiplayerBridge();

    private MultiplayerBridge() {}

    /**
     * Initialize multiplayer mode with a server address.
     * @param serverAddress Server address (e.g., "localhost:8080" or "ws://localhost:8080/webmc")
     */
    public void initMultiplayer(String serverAddress) {
        doInitMultiplayer(serverAddress);
    }

    /**
     * Connect to a multiplayer server.
     * @param address Server address
     * @param onOpen Callback when connected
     * @param onMessage Callback when message received
     * @param onClose Callback when closed
     * @param onError Callback on error
     */
    public void connect(String address, Runnable onOpen, MessageCallback onMessage,
                        CloseCallback onClose, ErrorCallback onError) {
        doConnect(address, onOpen, onMessage, onClose, onError);
    }

    /**
     * Send a raw message through the current connection.
     */
    public void sendRaw(String message) {
        doSendRaw(message);
    }

    /**
     * Send a position update.
     */
    public void sendPosition(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        doSendPosition(x, y, z, yaw, pitch, onGround);
    }

    /**
     * Send a chat message.
     */
    public void sendChat(String message) {
        doSendChat(message);
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        doDisconnect();
    }

    /**
     * Get current connection state.
     * @return "disconnected", "connecting", "connected", "closing", or "closed"
     */
    public String getConnectionState() {
        return doGetState();
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return "connected".equals(doGetState());
    }

    /**
     * Add a JavaScript event listener.
     */
    public void addEventListener(String event, JSEventHandler handler) {
        doAddEventListener(event, handler);
    }

    /**
     * Remove a JavaScript event listener.
     */
    public void removeEventListener(String event, JSEventHandler handler) {
        doRemoveEventListener(event, handler);
    }

    /**
     * Show the multiplayer menu UI.
     */
    public void showMultiplayerMenu() {
        doShowMenu();
    }

    /**
     * Hide the multiplayer menu UI.
     */
    public void hideMultiplayerMenu() {
        doHideMenu();
    }

    /**
     * Connect to a server by address.
     * @param address Server address (e.g., "localhost:8080")
     */
    public void connectToServer(String address) {
        doConnectToServer(address);
    }

    /**
     * Disconnect from the current server.
     */
    public void disconnect() {
        doDisconnect();
    }

    /**
     * Get current server address.
     */
    public String getCurrentServer() {
        return doGetCurrentServer();
    }

    // JavaScript interface methods

    @JSBody(script = """
        function(address) {
            if (window.SocketRedirect) {
                SocketRedirect.initMultiplayer(address);
            }
        }
        """)
    private native void doInitMultiplayer(String address);

    @JSBody(callbacks = true, script = """
        function(address, onOpen, onMessage, onClose, onError) {
            if (window.SocketRedirect) {
                var ws = SocketRedirect.connectToServer(address, {
                    onOpen: function(websocket) {
                        onOpen.run();
                    },
                    onMessage: function(data) {
                        onMessage.onMessage(data);
                    },
                    onClose: function(code, reason) {
                        onClose.onClose(code, reason);
                    },
                    onError: function(error) {
                        onError.onError(error && error.type ? error.type : 'Unknown error');
                    }
                });
                return ws;
            }
            onError.onError('SocketRedirect not available');
            return null;
        }
        """)
    private native void doConnect(String address, Runnable onOpen, MessageCallback onMessage,
                                  CloseCallback onClose, ErrorCallback onError);

    @JSBody(script = """
        function(message) {
            if (window.SocketRedirect && window.SocketRedirect._ws) {
                window.SocketRedirect._ws.send(message);
            }
        }
        """)
    private native void doSendRaw(String message);

    @JSBody(script = """
        function(x, y, z, yaw, pitch, onGround) {
            if (window.SocketRedirect) {
                SocketRedirect.sendPosition(x, y, z, yaw, pitch, onGround);
            }
        }
        """)
    private native void doSendPosition(double x, double y, double z, float yaw, float pitch, boolean onGround);

    @JSBody(script = """
        function(message) {
            if (window.SocketRedirect) {
                SocketRedirect.sendChat(message);
            }
        }
        """)
    private native void doSendChat(String message);

    @JSBody(script = """
        function() {
            if (window.SocketRedirect && window.SocketRedirect._ws) {
                window.SocketRedirect._ws.close();
            }
        }
        """)
    private native void doDisconnect();

    @JSBody(script = """
        function() {
            if (window.SocketRedirect) {
                return SocketRedirect.getState();
            }
            return 'disconnected';
        }
        """)
    private native String doGetState();

    @JSBody(callbacks = true, script = """
        function(event, handler) {
            if (window.WebMC) {
                window.WebMC.addEventListener(event, function(data) {
                    handler.handle(data);
                });
            }
        }
        """)
    private native void doAddEventListener(String event, JSEventHandler handler);

    @JSBody(callbacks = true, script = """
        function(event, handler) {
            if (window.WebMC) {
                window.WebMC.removeEventListener(event, function(data) {
                    handler.handle(data);
                });
            }
        }
        """)
    private native void doRemoveEventListener(String event, JSEventHandler handler);

    @JSBody(script = """
        function() {
            if (window.WebMCMultiplayer) {
                window.WebMCMultiplayer.showMenu();
            }
        }
        """)
    private native void doShowMenu();

    @JSBody(script = """
        function() {
            if (window.WebMCMultiplayer) {
                window.WebMCMultiplayer.hideMenu();
            }
        }
        """)
    private native void doHideMenu();

    @JSBody(script = """
        function(address) {
            if (window.SocketRedirect) {
                SocketRedirect.initMultiplayer(address);
            } else if (window.WebMCMultiplayer) {
                window.WebMCMultiplayer.showMenu();
            }
        }
        """)
    private native void doConnectToServer(String address);

    @JSBody(script = """
        function() {
            if (window.SocketRedirect && window.SocketRedirect._ws) {
                window.SocketRedirect._ws.close();
            }
        }
        """)
    private native void doDisconnect();

    @JSBody(script = """
        function() {
            if (window.WebMCMultiplayer && window.WebMCMultiplayer.getServerList) {
                var servers = window.WebMCMultiplayer.getServerList();
                if (servers && servers.length > 0) {
                    return servers[0].address;
                }
            }
            return '';
        }
        """)
    private native String doGetCurrentServer();

    // Callback interfaces

    @JSBody(callbacks = true, script = "({ run: function() {} })")
    public interface Runnable extends JSObject {
        void run();
    }

    @JSBody(callbacks = true, script = "({ onMessage: function(data) {} })")
    public interface MessageCallback extends JSObject {
        void onMessage(String data);
    }

    @JSBody(callbacks = true, script = "({ onClose: function(code, reason) {} })")
    public interface CloseCallback extends JSObject {
        void onClose(int code, String reason);
    }

    @JSBody(callbacks = true, script = "({ onError: function(error) {} })")
    public interface ErrorCallback extends JSObject {
        void onError(String error);
    }

    @JSBody(callbacks = true, script = "({ handle: function(data) {} })")
    public interface JSEventHandler extends JSObject {
        void handle(JSObject data);
    }
}