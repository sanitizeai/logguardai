package com.logguardai.metrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory registry for pattern-based metrics.
 * Maintains counters for each metric pattern and label combination.
 * Thread-safe with configurable cardinality limits.
 */
public class MetricsRegistry {
    
    private final List<MetricsPattern> patterns;
    private final Map<String, AtomicLong> counters;  // metric_key -> count
    private final Map<String, Integer> patternCardinalityCount;  // pattern_name -> unique label combos
    private final int maxCardinalityPerPattern;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private long lastFlushTime;

    public MetricsRegistry(int maxCardinalityPerPattern) {
        this.patterns = new ArrayList<>();
        this.counters = new ConcurrentHashMap<>();
        this.patternCardinalityCount = new ConcurrentHashMap<>();
        this.maxCardinalityPerPattern = maxCardinalityPerPattern;
        this.lastFlushTime = System.currentTimeMillis();
    }

    /**
     * Add a pattern to the registry.
     */
    public void addPattern(MetricsPattern pattern) {
        if (pattern == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            patterns.add(pattern);
            patternCardinalityCount.put(pattern.getPatternName(), 0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add multiple patterns.
     */
    public void addPatterns(List<MetricsPattern> newPatterns) {
        if (newPatterns == null || newPatterns.isEmpty()) {
            return;
        }
        lock.writeLock().lock();
        try {
            for (MetricsPattern pattern : newPatterns) {
                if (pattern != null) {
                    patterns.add(pattern);
                    patternCardinalityCount.put(pattern.getPatternName(), 0);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Record a log line against all patterns.
     * Increments counters for matching patterns.
     */
    public void recordLogLine(String logLine) {
        if (logLine == null || logLine.isEmpty()) {
            return;
        }

        lock.readLock().lock();
        try {
            for (MetricsPattern pattern : patterns) {
                if (!pattern.isEnabled()) {
                    continue;
                }

                Map<String, String> labels = pattern.match(logLine);
                if (labels != null) {
                    String metricKey = pattern.buildMetricKey(labels);
                    
                    // Check cardinality limit
                    String patternName = pattern.getPatternName();
                    int currentCardinality = patternCardinalityCount.getOrDefault(patternName, 0);
                    
                    if (!counters.containsKey(metricKey) && currentCardinality >= maxCardinalityPerPattern) {
                        // Skip this combination to prevent cardinality explosion
                        continue;
                    }
                    
                    // Increment counter
                    counters.computeIfAbsent(metricKey, k -> new AtomicLong(0)).incrementAndGet();
                    
                    // Update cardinality count if this is a new combination
                    if (currentCardinality < maxCardinalityPerPattern) {
                        patternCardinalityCount.put(patternName, currentCardinality + 1);
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get current metrics as map.
     */
    public Map<String, Long> getMetrics() {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }

    /**
     * Get count for a specific metric.
     */
    public long getCount(String metricKey) {
        AtomicLong count = counters.get(metricKey);
        return count != null ? count.get() : 0;
    }

    /**
     * Increment a metric by a specific value.
     */
    public void incrementMetric(String metricKey, long delta) {
        counters.computeIfAbsent(metricKey, k -> new AtomicLong(0)).addAndGet(delta);
    }

    /**
     * Reset all metrics (clear counters).
     */
    public void reset() {
        lock.writeLock().lock();
        try {
            counters.clear();
            patternCardinalityCount.replaceAll((k, v) -> 0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reset metrics for a specific pattern.
     */
    public void resetPattern(String patternName) {
        lock.writeLock().lock();
        try {
            counters.entrySet().removeIf(entry -> {
                String key = entry.getKey();
                // Simple check: if key starts with pattern's metric name
                for (MetricsPattern pattern : patterns) {
                    if (pattern.getPatternName().equals(patternName)) {
                        return key.startsWith(pattern.getMetricName());
                    }
                }
                return false;
            });
            patternCardinalityCount.put(patternName, 0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get number of active metrics tracked.
     */
    public int getMetricsCount() {
        return counters.size();
    }

    /**
     * Get number of patterns.
     */
    public int getPatternCount() {
        lock.readLock().lock();
        try {
            return patterns.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get all patterns.
     */
    public List<MetricsPattern> getPatterns() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(patterns);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get cardinality stats for debugging.
     */
    public Map<String, Integer> getCardinalityStats() {
        return new HashMap<>(patternCardinalityCount);
    }

    /**
     * Update last flush time (called by writer).
     */
    public void updateFlushTime() {
        this.lastFlushTime = System.currentTimeMillis();
    }

    /**
     * Get last flush time.
     */
    public long getLastFlushTime() {
        return lastFlushTime;
    }

    /**
     * Get elapsed time since last flush (in milliseconds).
     */
    public long getTimeSinceLastFlush() {
        return System.currentTimeMillis() - lastFlushTime;
    }
}
