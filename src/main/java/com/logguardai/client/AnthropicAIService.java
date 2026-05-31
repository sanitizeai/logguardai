package com.logguardai.client;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;
import com.logguardai.ai.AIServiceException;

/**
 * Anthropic Claude AI service implementation.
 *
 * Supports Claude models (claude-3-sonnet-20240229, claude-3-haiku-20240307, etc.)
 * Uses HTTP/JSON for direct API calls (no external dependencies).
 */
public class AnthropicAIService implements AIService {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_API_VERSION = "2023-06-01";

    private final AIConfig config;
    private boolean healthy = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public AnthropicAIService(AIConfig config) {
        this.config = config;
        if (config.isConfigured()) {
            this.healthy = checkHealth();
        }
    }

    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Anthropic service not healthy");
        }

        String prompt = String.format(
            "You are a data sanitization expert. Sanitize this sensitive value without revealing its type or content. " +
            "Return ONLY a sanitized version (e.g., '[REDACTED]', '[MASKED]', a hash-like string, or generic placeholder).\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        return callAnthropic(prompt, "Sanitize value", config.getTimeoutMs());
    }

    @Override
    public String explainException(String exceptionType, String message, String stackTraceSnippet)
            throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Anthropic service not healthy");
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

        return callAnthropic(prompt, "Explain exception", config.getTimeoutMs());
    }

    @Override
    public String classifyData(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Anthropic service not healthy");
        }

        String prompt = String.format(
            "Classify this data as one of: 'pii' (personally identifiable), 'sensitive' (confidential), 'public' (safe), or 'unknown'.\n" +
            "Return ONLY the classification.\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        String result = callAnthropic(prompt, "Classify data", config.getTimeoutMs());
        return result.toLowerCase().trim();
    }

    @Override
    public Map<String, String> sanitizeBatch(List<String> values, List<String> contexts) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Anthropic service not healthy");
        }

        if (values == null || values.isEmpty()) {
            return new HashMap<>();
        }

        // Build batch prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a data sanitization expert. Sanitize these sensitive values without revealing their type or content.\n");
        prompt.append("Return ONLY a JSON object where each key is the original value and the value is the sanitized version.\n");
        prompt.append("Use generic placeholders like '[REDACTED]', '[MASKED]', hash-like strings, etc.\n\n");

        for (int i = 0; i < values.size(); i++) {
            String context = (contexts != null && i < contexts.size()) ? contexts.get(i) : "unknown";
            prompt.append(String.format("Field %d: %s\nValue %d: %s\n\n", i + 1, context, i + 1, values.get(i)));
        }

        prompt.append("Return format: {\"original_value1\": \"sanitized1\", \"original_value2\": \"sanitized2\", ...}");

        String response = callAnthropic(prompt.toString(), "Batch sanitize", config.getTimeoutMs());

        try {
            // Parse JSON response
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return result;
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse batch sanitization response: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> classifyDataBatch(List<String> values, List<String> contexts) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Anthropic service not healthy");
        }

        if (values == null || values.isEmpty()) {
            return new HashMap<>();
        }

        // Build batch prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("Classify these data values as one of: 'pii' (personally identifiable), 'sensitive' (confidential), 'public' (safe), or 'unknown'.\n");
        prompt.append("Return ONLY a JSON object where each key is the original value and the value is the classification.\n\n");

        for (int i = 0; i < values.size(); i++) {
            String context = (contexts != null && i < contexts.size()) ? contexts.get(i) : "unknown";
            prompt.append(String.format("Field %d: %s\nValue %d: %s\n\n", i + 1, context, i + 1, values.get(i)));
        }

        prompt.append("Return format: {\"value1\": \"pii\", \"value2\": \"sensitive\", ...}");

        String response = callAnthropic(prompt.toString(), "Batch classify", config.getTimeoutMs());

        try {
            // Parse JSON response
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString().toLowerCase().trim());
            }
            return result;
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse batch classification response: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<String> sanitizeAsync(String value, String context) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Anthropic service not healthy"));
            return failed;
        }

        String prompt = String.format(
            "You are a data sanitization expert. Sanitize this sensitive value without revealing its type or content. " +
            "Return ONLY a sanitized version (e.g., '[REDACTED]', '[MASKED]', a hash-like string, or generic placeholder).\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        return CompletableFuture.supplyAsync(() -> {
            try {
                return callAnthropic(prompt, "Sanitize value", config.getTimeoutMs());
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("API call failed: " + e.getMessage(), e));
            }
        }, executor);
    }

    @Override
    public CompletableFuture<String> explainExceptionAsync(String exceptionType, String message, String stackTraceSnippet) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Anthropic service not healthy"));
            return failed;
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

        return CompletableFuture.supplyAsync(() -> {
            try {
                return callAnthropic(prompt, "Explain exception", config.getTimeoutMs());
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("API call failed: " + e.getMessage(), e));
            }
        }, executor);
    }

    @Override
    public CompletableFuture<String> classifyDataAsync(String value, String context) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Anthropic service not healthy"));
            return failed;
        }

        String prompt = String.format(
            "Classify this data as one of: 'pii' (personally identifiable), 'sensitive' (confidential), 'public' (safe), or 'unknown'.\n" +
            "Return ONLY the classification.\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        return CompletableFuture.supplyAsync(() -> {
            try {
                return callAnthropic(prompt, "Classify data", config.getTimeoutMs());
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("API call failed: " + e.getMessage(), e));
            }
        }, executor)
          .thenApply(String::toLowerCase);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public String getServiceName() {
        return String.format("Anthropic Claude (%s)", config.getModel());
    }

    /**
     * Call Anthropic API with timeout protection.
     */
    private String callAnthropic(String prompt, String taskName, long timeoutMs) throws AIServiceException {
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
     * Make actual HTTP call to Anthropic API.
     */
    private String makeAPICall(String prompt) throws Exception {
        URL url = new URL(ANTHROPIC_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Setup request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", config.getApiKey());
            conn.setRequestProperty("anthropic-version", ANTHROPIC_API_VERSION);
            conn.setDoOutput(true);
            conn.setConnectTimeout((int) config.getTimeoutMs());
            conn.setReadTimeout((int) config.getTimeoutMs());

            // Build request body for Anthropic Messages API
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
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
     * Extract text content from Anthropic API response.
     */
    @SuppressWarnings("deprecation")
    private String extractContent(String response) throws Exception {
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        JsonArray content = json.getAsJsonArray("content");
        if (content == null || content.size() == 0) {
            throw new Exception("No content in response");
        }

        JsonObject contentObj = content.get(0).getAsJsonObject();
        String text = contentObj.get("text").getAsString();
        return text.trim();
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
     * Check if Anthropic service is accessible.
     */
    private boolean checkHealth() {
        try {
            String result = callAnthropic("Reply 'OK'", "Health check", 5000);
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            System.err.println("Anthropic health check failed: " + e.getMessage());
            return false;
        }
    }
}