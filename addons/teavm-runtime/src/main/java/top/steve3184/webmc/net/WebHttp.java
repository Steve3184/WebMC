package top.steve3184.webmc.net;

import java.util.concurrent.CompletableFuture;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * Browser-side HTTP helper, sibling of
 * {@link top.steve3184.webmc.vfs.WebFs}. Currently only used as a sync XHR
 * primitive - bigger plans:
 *
 * <ul>
 *   <li>Replace {@code java.net.URL.openConnection()} usage in MC's
 *       remaining HTTP paths (download server packs, telemetry uploads,
 *       Mojang services). Each call goes through {@link #get} / {@link #post}
 *       so we can swap in a real backend later.</li>
 *   <li>Power a real Microsoft auth flow once we run an offscreen popup or
 *       postMessage handshake - the resulting access token replaces
 *       {@link OfflineSessionService} for online play.</li>
 * </ul>
 *
 * <p>Synchronous semantics for now: TeaVM's main thread can park on a sync
 * XHR without blocking the browser permanently because we're inside a Web
 * Worker (WebMain runs in a thread spawned by TeaVM's runtime). If we
 * later move to an async/promise model, this class will offer matching
 * non-blocking methods.</p>
 */
public final class WebHttp {

    /** GET a URL synchronously. Returns response body bytes, or null on non-2xx. */
    public static byte[] get(String url) {
        try {
            return doGet(url);
        } catch (Throwable t) {
            log("get " + url + " failed: " + t);
            return null;
        }
    }

    /** POST a URL with a body. Returns response body bytes, or null on non-2xx. */
    public static byte[] post(String url, String contentType, byte[] body) {
        try {
            return doPost(url, contentType == null ? "application/octet-stream" : contentType, body);
        } catch (Throwable t) {
            log("post " + url + " failed: " + t);
            return null;
        }
    }

    /**
     * Async GET request returning a CompletableFuture.
     * This is the preferred method for chunk streaming as it doesn't block the main thread.
     */
    public static CompletableFuture<byte[]> getAsync(String url) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        doGetAsync(url, new JSObject() {
            @SuppressWarnings("unused")
            public void success(byte[] data) {
                future.complete(data);
            }

            @SuppressWarnings("unused")
            public void error(String error) {
                log("getAsync " + url + " error: " + error);
                future.completeExceptionally(new RuntimeException("HTTP GET failed: " + error));
            }
        });

        return future;
    }

    /**
     * Async batch GET for multiple URLs with timeout.
     * More efficient than individual async calls as it can use connection pooling.
     */
    public static CompletableFuture<byte[]> getBatchAsync(String url, int timeoutMs) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        doGetAsyncWithTimeout(url, timeoutMs, new JSObject() {
            @SuppressWarnings("unused")
            public void success(byte[] data) {
                future.complete(data);
            }

            @SuppressWarnings("unused")
            public void error(String error) {
                log("getBatchAsync " + url + " error: " + error);
                future.completeExceptionally(new RuntimeException("HTTP GET failed: " + error));
            }
        });

        return future;
    }

    @JSBody(params = "url", script =
        "var x = new XMLHttpRequest();" +
        "x.open('GET', url, false);" +
        "x.overrideMimeType('text/plain; charset=x-user-defined');" +
        "x.send(null);" +
        "if (x.status < 200 || x.status >= 300) return null;" +
        "var s = x.responseText, n = s.length;" +
        "var arr = new Int8Array(n);" +
        "for (var i = 0; i < n; i++) arr[i] = s.charCodeAt(i) & 0xff;" +
        "return arr;")
    private static native byte[] doGet(String url);

    @JSBody(params = {"url", "contentType", "body"}, script =
        "var x = new XMLHttpRequest();" +
        "x.open('POST', url, false);" +
        "x.setRequestHeader('Content-Type', contentType);" +
        "x.overrideMimeType('text/plain; charset=x-user-defined');" +
        "x.send(body);" +
        "if (x.status < 200 || x.status >= 300) return null;" +
        "var s = x.responseText, n = s.length;" +
        "var arr = new Int8Array(n);" +
        "for (var i = 0; i < n; i++) arr[i] = s.charCodeAt(i) & 0xff;" +
        "return arr;")
    private static native byte[] doPost(String url, String contentType, byte[] body);

    /**
     * Async GET implementation using callbacks.
     * callback.success(data) or callback.error(message)
     */
    @JSBody(params = {"url", "callback"}, script =
        "var x = new XMLHttpRequest();" +
        "x.open('GET', url, true);" +
        "x.overrideMimeType('text/plain; charset=x-user-defined');" +
        "x.onload = function() {" +
        "  if (x.status >= 200 && x.status < 300) {" +
        "    var s = x.responseText, n = s.length;" +
        "    var arr = new Int8Array(n);" +
        "    for (var i = 0; i < n; i++) arr[i] = s.charCodeAt(i) & 0xff;" +
        "    callback.success(arr);" +
        "  } else {" +
        "    callback.error('HTTP ' + x.status);" +
        "  }" +
        "};" +
        "x.onerror = function() { callback.error('Network error'); };" +
        "x.send(null);")
    private static native void doGetAsync(String url, JSObject callback);

    /**
     * Async GET with timeout implementation.
     */
    @JSBody(params = {"url", "timeoutMs", "callback"}, script =
        "var x = new XMLHttpRequest();" +
        "x.open('GET', url, true);" +
        "x.overrideMimeType('text/plain; charset=x-user-defined');" +
        "x.timeout = timeoutMs;" +
        "x.ontimeout = function() { callback.error('Timeout'); };" +
        "x.onload = function() {" +
        "  if (x.status >= 200 && x.status < 300) {" +
        "    var s = x.responseText, n = s.length;" +
        "    var arr = new Int8Array(n);" +
        "    for (var i = 0; i < n; i++) arr[i] = s.charCodeAt(i) & 0xff;" +
        "    callback.success(arr);" +
        "  } else {" +
        "    callback.error('HTTP ' + x.status);" +
        "  }" +
        "};" +
        "x.onerror = function() { callback.error('Network error'); };" +
        "x.send(null);")
    private static native void doGetAsyncWithTimeout(String url, int timeoutMs, JSObject callback);

    @JSBody(params = "msg", script = "console.log('[mc-web/net] ' + msg);")
    private static native void log(String msg);

    private WebHttp() {}
}