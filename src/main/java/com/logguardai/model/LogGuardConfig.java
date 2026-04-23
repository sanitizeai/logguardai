package com.logguardai.model;

/**
 * Configuration for LogGuardAI sanitization pipeline.
 */
public class LogGuardConfig {
    private boolean aiEnabled;
    private int aiThreshold;
    private long timeoutMs;
    private double samplingRate;

    public LogGuardConfig() {
        this.aiEnabled = false;
        this.aiThreshold = 5;
        this.timeoutMs = 150;
        this.samplingRate = 0.05;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public int getAiThreshold() {
        return aiThreshold;
    }

    public void setAiThreshold(int aiThreshold) {
        this.aiThreshold = aiThreshold;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(double samplingRate) {
        this.samplingRate = samplingRate;
    }
}
