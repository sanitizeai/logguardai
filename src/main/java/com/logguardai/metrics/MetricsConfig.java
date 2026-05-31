package com.logguardai.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for pattern-based metrics system.
 */
public class MetricsConfig {
    
    private boolean enabled;
    private String filePath;
    private long flushIntervalMs;
    private int maxCardinalityPerPattern;
    private List<MetricsPattern> patterns;

    public MetricsConfig() {
        this.enabled = false;
        this.filePath = "logs/metrics.txt";
        this.flushIntervalMs = 60000;  // 1 minute
        this.maxCardinalityPerPattern = 10000;  // Max 10k unique label combos per pattern
        this.patterns = new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public int getMaxCardinalityPerPattern() {
        return maxCardinalityPerPattern;
    }

    public void setMaxCardinalityPerPattern(int maxCardinalityPerPattern) {
        this.maxCardinalityPerPattern = maxCardinalityPerPattern;
    }

    public List<MetricsPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<MetricsPattern> patterns) {
        this.patterns = patterns != null ? patterns : new ArrayList<>();
    }

    public void addPattern(MetricsPattern pattern) {
        if (pattern != null) {
            this.patterns.add(pattern);
        }
    }

    /**
     * Check if metrics are properly configured.
     */
    public boolean isConfigured() {
        return enabled && !patterns.isEmpty() && filePath != null && !filePath.isEmpty();
    }

    @Override
    public String toString() {
        return "MetricsConfig{" +
                "enabled=" + enabled +
                ", filePath='" + filePath + '\'' +
                ", flushIntervalMs=" + flushIntervalMs +
                ", maxCardinality=" + maxCardinalityPerPattern +
                ", patterns=" + patterns.size() +
                '}';
    }
}
