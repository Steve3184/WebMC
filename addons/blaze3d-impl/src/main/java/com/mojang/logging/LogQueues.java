package com.mojang.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nullable;

/** Browser-side replacement for com.mojang.logging.LogQueues. No threading
 *  in JS, but we keep the BlockingQueue-of-strings interface so callers compile. */
public class LogQueues {
    private static final Map<String, BlockingQueue<String>> QUEUES = new HashMap<>();

    public static BlockingQueue<String> getOrCreateQueue(final String target) {
        return QUEUES.computeIfAbsent(target, k -> new LinkedBlockingQueue<>());
    }

    @Nullable
    public static String getNextLogEvent(final String queueName) {
        BlockingQueue<String> q = QUEUES.get(queueName);
        if (q != null) return q.poll();
        return null;
    }
}
