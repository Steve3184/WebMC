package com.mojang.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.event.Level;

/** Browser-side replacement for com.mojang.logging.LogListeners. The real
 *  one bridges log4j-core LogEvent into SLF4J listeners; we drop log4j
 *  entirely so Target.post() is a no-op (listeners are still tracked but
 *  never fire — log4j-core never reaches us). */
public class LogListeners {
    private static final Map<String, Target> TARGETS = new ConcurrentHashMap<>();

    public static Target getOrCreateTarget(final String target) {
        return TARGETS.computeIfAbsent(target, s -> new Target());
    }

    public static void addListener(final String target, final Listener listener) {
        getOrCreateTarget(target).addListener(listener);
    }

    public static class Target {
        private volatile List<Listener> listeners = List.of();

        private synchronized void addListener(final Listener listener) {
            final List<Listener> newListeners = new ArrayList<>(listeners.size() + 1);
            newListeners.addAll(listeners);
            newListeners.add(listener);
            listeners = newListeners;
        }

        // Real signature uses log4j-core types; we keep the API but never
        // actually pump events through it — log4j is inert in the browser.
        public void post(final Object layout, final Object event) {
            // no-op; listeners are kept for API compatibility
        }
    }

    public interface Listener {
        void accept(String message, Level level);
    }
}
