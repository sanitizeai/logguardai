package com.logguardai.ai;

import java.util.List;

/**
 * Configuration for AI service integration.
 */
public class AIConfig {
    
    private String apiKey;
    private String apiProvider;          // "openai", "anthropic", "azure-openai", "ollama", "onnx"
    private String model;                 // "gpt-3.5-turbo", "gpt-4", "claude-3-sonnet-20240229", etc.
    private String azureEndpoint;         // Azure OpenAI endpoint URL
    private String azureDeployment;       // Azure OpenAI deployment name
    private String azureApiVersion;       // Azure OpenAI API version
    private String ollamaEndpoint;        // Ollama local endpoint URL
    private String onnxModelPath;         // Local ONNX model file path
    private int maxTokens;
    private double temperature;
    private long timeoutMs;
    private boolean retryOnTimeout;
    private int maxRetries;
    private List<String> safeKeyPatterns; // Regex patterns for keys that should not be sanitized

    public AIConfig() {
        this.apiProvider = "openai";
        this.model = "gpt-3.5-turbo";
        this.maxTokens = 150;
        this.temperature = 0.3;           // Low temperature for consistency
        this.timeoutMs = 2000;            // 2 second timeout
        this.retryOnTimeout = false;
        this.maxRetries = 1;
        this.ollamaEndpoint = "http://localhost:11434";
        this.onnxModelPath = "";
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

    public String getAzureEndpoint() {
        return azureEndpoint;
    }

    public void setAzureEndpoint(String azureEndpoint) {
        this.azureEndpoint = azureEndpoint;
    }

    public String getAzureDeployment() {
        return azureDeployment;
    }

    public void setAzureDeployment(String azureDeployment) {
        this.azureDeployment = azureDeployment;
    }

    public String getAzureApiVersion() {
        return azureApiVersion;
    }

    public void setAzureApiVersion(String azureApiVersion) {
        this.azureApiVersion = azureApiVersion;
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

    public List<String> getSafeKeyPatterns() {
        return safeKeyPatterns;
    }

    public void setSafeKeyPatterns(List<String> safeKeyPatterns) {
        this.safeKeyPatterns = safeKeyPatterns;
    }

    public String getOllamaEndpoint() {
        return ollamaEndpoint;
    }

    public void setOllamaEndpoint(String ollamaEndpoint) {
        this.ollamaEndpoint = ollamaEndpoint;
    }

    public String getOnnxModelPath() {
        return onnxModelPath;
    }

    public void setOnnxModelPath(String onnxModelPath) {
        this.onnxModelPath = onnxModelPath;
    }

    public boolean isConfigured() {
        if ("ollama".equalsIgnoreCase(apiProvider) || "onnx".equalsIgnoreCase(apiProvider)) {
            return true;
        }
        return apiKey != null && !apiKey.isEmpty();
    }
}
