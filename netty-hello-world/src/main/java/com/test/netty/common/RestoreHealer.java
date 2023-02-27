package com.test.netty.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class RestoreHealer {
    private static Logger logger = LoggerFactory.getLogger(RestoreHealer.class);

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static ConcurrentHashMap<Runnable, Integer> callbacks = new ConcurrentHashMap();

    private static volatile boolean started = false;

    private static String RESTORE0 = "/databricks/RESTORE0";
    private static volatile boolean restored0 = false;
    private static String RESTORE1 = "/databricks/RESTORE1";
    private static volatile boolean restored1 = false;


    private static void start() {
        executorService.scheduleAtFixedRate(() -> {
            File restore0 = new File(RESTORE0);
            File restore1 = new File(RESTORE1);
            if (restore0.exists() && !restored0) {
                heal(0);
                restored0 = true;
            }
            if (restore1.exists() && !restored1) {
                heal(1);
                restored1 = true;
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private static void heal(int targetPriority) {
        for (Map.Entry<Runnable, Integer> entry : callbacks.entrySet()) {
            if (entry.getValue().equals(targetPriority)) {
                try {
                    entry.getKey().run();
                } catch (Exception e) {
                    logger.warn("Failed to invoke callback " + entry.getKey());
                }
            }
        }
    }

    public static void registerCallback(Runnable cb, int priority) {
        if (!started) {
            start();
            started = true;
        }
        callbacks.put(cb, priority);
    }
}
