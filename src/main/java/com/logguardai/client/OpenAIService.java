package com.logguardai.client;

import com.google.gson.*;
import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;
import com.logguardai.ai.AIServiceException;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * OpenAI GPT-based AI service implementation.
 * 
 * Supports GPT-3.5-turbo and GPT-4 models with timeout protection.
 * Uses HTTP/JSON for direct API calls (no external dependencies).
 */
public class OpenAIService implements AIService {
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_API_VERSION = "v1";
    
    private final AIConfig config;
    private boolean healthy = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public OpenAIService(AIConfig config) {
        this.config = config;
        if (config.isConfigured()) {
            this.healthy = checkHealth();
        }
    }

    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("OpenAI service not healthy");
        }

        String prompt = String.format(
            "You are a data sanitization expert. Sanitize this sensitive value without revealing its type or content. " +
            "Return ONLY a sanitized version (e.g., '[REDACTED]', '[MASKED]', a hash-like string, or generic placeholder).\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        return callOpenAI(prompt, "Sanitize value", config.getTimeoutMs());
    }

    @Override
    public String explainException(String exceptionType, String message, String stackTraceSnippet) 
            throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("OpenAI service not healthy");
        }

        String prompt = String.format(
            "Provide a brief, developer-friendly explanation for this exception.\n" +
            "Include: likely cause, what went wrong, and a 1-line fix suggestion.\n" +
            "Keep it under 100 words.\n\n" +
            "Exception: %s\n" +
            "Message: %s\n" +
            "Stack: %s",
            exceptionType, message, stackTraceSnippet
        );

        return callOpenAI(prompt, "Explain exception", config.getTimeoutMs());
    }

    @Override
    public String classifyData(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("OpenAI service not healthy");
        }

        String prompt = String.format(
            "Classify this data as one of: 'pii' (personally identifiable), 'sensitive' (confidential), 'public' (safe), or 'unknown'.\n" +
            "Return ONLY the classification.\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        String result = callOpenAI(prompt, "Classify data", config.getTimeoutMs());
        return result.toLowerCase().trim();
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public String getServiceName() {
        return String.format("OpenAI %s (model: %s)", OPENAI_API_VERSION, config.getModel());
    }

    /**
     * Call OpenAI API with timeout protection.
     */
    private String callOpenAI(String prompt, String taskName, long timeoutMs) throws AIServiceException {
        try {
            Future<String> future = executor.submit(() -> {
                try {
                    return makeAPICall(prompt);
                } catch (Exception e) {
                    throw new AIServiceException("API call failed: " + e.getMessage(), e);
                }
            });

            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new AIServiceException(taskName + " timed out", timeoutMs);
            }

        } catch (InterruptedException e) {
            throw new AIServiceException("Interrupted while " + taskName, e);
        } catch (ExecutionException e) {
            throw new AIServiceException("Execution error: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /**
     * Make actual HTTP call to OpenAI API.
     */
    private String makeAPICall(String prompt) throws Exception {
        URL url = new URL(OPENAI_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Setup request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout((int) config.getTimeoutMs());
            conn.setReadTimeout((int) config.getTimeoutMs());

            // Build request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
            requestBody.addProperty("temperature", config.getTemperature());
            requestBody.addProperty("max_tokens", config.getMaxTokens());

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);

            requestBody.add("messages", messages);

            // Send request
            String requestJson = requestBody.toString();
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int status = conn.getResponseCode();
            if (status != 200) {
                String error = readStream(conn.getErrorStream());
                throw new Exception("API error " + status + ": " + error);
            }

            String response = readStream(conn.getInputStream());
            return extractContent(response);

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Extract text content from OpenAI API response.
     */
    @SuppressWarnings("deprecation")
    private String extractContent(String response) throws Exception {
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new Exception("No choices in response");
        }

        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject message = choice.getAsJsonObject("message");
        String content = message.get("content").getAsString();
        return content.trim();
    }

    /**
     * Read input stream to string.
     */
    private String readStream(java.io.InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Check if OpenAI service is accessible.
     */
    private boolean checkHealth() {
        try {
            String result = callOpenAI("Reply 'OK'", "Health check", 5000);
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            System.err.println("OpenAI health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Shutdown the executor.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
