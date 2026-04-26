package com.logguardai.ai;

/**
 * Configuration for AI service integration.
 */
public class AIConfig {
    
    private String apiKey;
    private String apiProvider;          // "openai", "anthropic", etc.
    private String model;                 // "gpt-3.5-turbo", "gpt-4", etc.
    private int maxTokens;
    private double temperature;
    private long timeoutMs;
    private boolean retryOnTimeout;
    private int maxRetries;

    public AIConfig() {
        this.apiProvider = "openai";
        this.model = "gpt-3.5-turbo";
        this.maxTokens = 150;
        this.temperature = 0.3;           // Low temperature for consistency
        this.timeoutMs = 2000;            // 2 second timeout
        this.retryOnTimeout = false;
        this.maxRetries = 1;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isRetryOnTimeout() {
        return retryOnTimeout;
    }

    public void setRetryOnTimeout(boolean retryOnTimeout) {
        this.retryOnTimeout = retryOnTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
