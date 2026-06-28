package top.steve3184.webmc.net;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSString;
import org.teavm.jso.core.JSUndefined;

import java.util.function.Consumer;

/**
 * Browser WebSocket client for TeaVM.
 * Provides WebSocket connectivity for multiplayer functionality in the browser.
 */
public final class WebSocketClient {

    private String url;
    private boolean connected;
    private boolean connecting;

    private final WebSocketHandler handler;
    private JSObject ws;

    public interface WebSocketHandler {
        void onOpen();
        void onMessage(String data);
        void onError(String error);
        void onClose(int code, String reason);
    }

    public WebSocketClient(String url, WebSocketHandler handler) {
        this.url = url;
        this.handler = handler;
        this.connected = false;
        this.connecting = false;
    }

    /**
     * Connect to the WebSocket server.
     */
    public void connect() {
        if (connecting || connected) {
            return;
        }
        connecting = true;
        createAndConnect(url);
    }

    /**
     * Send a text message through the WebSocket.
     */
    public void send(String message) {
        if (connected && ws != null) {
            doSend(ws, message);
        }
    }

    /**
     * Send binary data through the WebSocket.
     */
    public void sendBinary(byte[] data) {
        if (connected && ws != null) {
            doSendBinary(ws, data);
        }
    }

    /**
     * Close the WebSocket connection.
     */
    public void close() {
        if (ws != null) {
            doClose(ws);
        }
        connected = false;
        connecting = false;
    }

    /**
     * Check if the WebSocket is connected.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Check if currently connecting.
     */
    public boolean isConnecting() {
        return connecting;
    }

    private void createAndConnect(String url) {
        WebSocketClient self = this;
        doCreateWebSocket(url, new WebSocketCallbacks() {
            @Override
            public void onCreated(JSObject webSocket) {
                ws = webSocket;
                doSetOnOpen(webSocket, () -> {
                    connected = true;
                    connecting = false;
                    if (handler != null) {
                        handler.onOpen();
                    }
                });
                doSetOnMessage(webSocket, (String data) -> {
                    if (handler != null) {
                        handler.onMessage(data);
                    }
                });
                doSetOnError(webSocket, (String error) -> {
                    connecting = false;
                    if (handler != null) {
                        handler.onError(error);
                    }
                });
                doSetOnClose(webSocket, (int code, String reason) -> {
                    connected = false;
                    connecting = false;
                    if (handler != null) {
                        handler.onClose(code, reason);
                    }
                });
            }

            @Override
            public void onError(String error) {
                connecting = false;
                if (handler != null) {
                    handler.onError(error);
                }
            }
        });
    }

    @JSBody(script = """
        function(url, callbacks) {
            try {
                var ws = new WebSocket(url);
                ws.binaryType = 'arraybuffer';
                callbacks.onCreated(ws);
            } catch (e) {
                callbacks.onError(e.message);
            }
        }
        """)
    private static native void doCreateWebSocket(String url, WebSocketCallbacks callbacks);

    @JSBody(script = """
        function(ws, handler) {
            ws.onopen = function() { handler.onOpen(); };
        }
        """)
    private static native void doSetOnOpen(JSObject ws, Runnable handler);

    @JSBody(script = """
        function(ws, handler) {
            ws.onmessage = function(event) {
                if (typeof event.data === 'string') {
                    handler.onMessage(event.data);
                } else if (event.data instanceof ArrayBuffer) {
                    var bytes = new Uint8Array(event.data);
                    var str = '';
                    for (var i = 0; i < bytes.length; i++) {
                        str += String.fromCharCode(bytes[i]);
                    }
                    handler.onMessage(str);
                }
            };
        }
        """)
    private static native void doSetOnMessage(JSObject ws, MessageHandler handler);

    @JSBody(script = """
        function(ws, handler) {
            ws.onerror = function(event) {
                handler.onError(event.type || 'WebSocket error');
            };
        }
        """)
    private static native void doSetOnError(JSObject ws, ErrorHandler handler);

    @JSBody(script = """
        function(ws, handler) {
            ws.onclose = function(event) {
                handler.onClose(event.code || 1005, event.reason || '');
            };
        }
        """)
    private static native void doSetOnClose(JSObject ws, CloseHandler handler);

    @JSBody(script = """
        function(ws, message) {
            if (ws.readyState === WebSocket.OPEN) {
                ws.send(message);
            }
        }
        """)
    private static native void doSend(JSObject ws, String message);

    @JSBody(script = """
        function(ws, data) {
            if (ws.readyState === WebSocket.OPEN) {
                ws.send(data);
            }
        }
        """)
    private static native void doSendBinary(JSObject ws, byte[] data);

    @JSBody(script = """
        function(ws) {
            ws.close();
        }
        """)
    private static native void doClose(JSObject ws);

    @JSBody(callbacks = true, script = """
        ({
            onCreated: function(ws) {},
            onError: function(error) {}
        })
        """)
    private interface WebSocketCallbacks extends JSObject {
        void onCreated(JSObject ws);
        void onError(String error);
    }

    @JSBody(callbacks = true, script = """
        ({
            onOpen: function() {},
            onMessage: function(data) {},
            onError: function(error) {},
            onClose: function(code, reason) {}
        })
        """)
    private interface WebSocketInitCallbacks extends JSObject {
        void onCreated(JSObject ws);
        void onError(String error);
    }

    private interface Runnable extends JSObject {
        void run();
    }

    private interface MessageHandler extends JSObject {
        void onMessage(String data);
    }

    private interface ErrorHandler extends JSObject {
        void onError(String error);
    }

    private interface CloseHandler extends JSObject {
        void onClose(int code, String reason);
    }

    private WebSocketClient() {}
}
