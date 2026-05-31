package com.logguardai.metrics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages periodic flushing of metrics to file.
 * Runs in a background thread with configurable interval.
 */
public class MetricsFlushManager {
    
    private final MetricsRegistry registry;
    private final MetricsFileWriter writer;
    private final long flushIntervalMs;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> flushTask;
    private volatile boolean running;

    public MetricsFlushManager(MetricsRegistry registry, MetricsFileWriter writer, long flushIntervalMs) {
        this.registry = registry;
        this.writer = writer;
        this.flushIntervalMs = flushIntervalMs;
        this.executor = Executors.newScheduledThreadPool(1, (runnable) -> {
            Thread thread = new Thread(runnable, "LogGuardAI-MetricsFlush");
            thread.setDaemon(true);
            return thread;
        });
        this.running = false;
    }

    /**
     * Start periodic flushing.
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        flushTask = executor.scheduleAtFixedRate(
                this::flush,
                flushIntervalMs,  // Initial delay
                flushIntervalMs,  // Period
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop periodic flushing.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (flushTask != null) {
            flushTask.cancel(false);
        }
        
        // Flush remaining metrics before shutdown
        flush();
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform immediate flush.
     */
    public void flush() {
        try {
            if (registry.getMetricsCount() > 0) {
                writer.flush();
            }
        } catch (Exception e) {
            System.err.println("LogGuardAI: Error flushing metrics: " + e.getMessage());
        }
    }

    /**
     * Check if manager is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get metrics registry.
     */
    public MetricsRegistry getRegistry() {
        return registry;
    }

    /**
     * Get metrics writer.
     */
    public MetricsFileWriter getWriter() {
        return writer;
    }
}
