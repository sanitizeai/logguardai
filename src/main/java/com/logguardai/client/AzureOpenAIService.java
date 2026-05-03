package com.logguardai.client;

import com.google.gson.*;
import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;
import com.logguardai.ai.AIServiceException;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Azure OpenAI service implementation.
 *
 * Supports Azure OpenAI deployments with custom endpoints.
 * Uses HTTP/JSON for direct API calls (no external dependencies).
 */
public class AzureOpenAIService implements AIService {

    private final AIConfig config;
    private boolean healthy = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final String azureEndpoint;
    private final String deploymentName;
    private final String apiVersion;

    public AzureOpenAIService(AIConfig config) {
        this.config = config;
        // Use Azure-specific configuration fields
        this.azureEndpoint = config.getAzureEndpoint() != null ? config.getAzureEndpoint() : "https://your-resource.openai.azure.com";
        this.deploymentName = config.getAzureDeployment() != null ? config.getAzureDeployment() : "gpt-35-turbo";
        this.apiVersion = config.getAzureApiVersion() != null ? config.getAzureApiVersion() : "2023-12-01";

        if (config.isConfigured()) {
            this.healthy = checkHealth();
        }
    }

    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Azure OpenAI service not healthy");
        }

        String prompt = String.format(
            "You are a data sanitization expert. Sanitize this sensitive value without revealing its type or content. " +
            "Return ONLY a sanitized version (e.g., '[REDACTED]', '[MASKED]', a hash-like string, or generic placeholder).\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        return callAzureOpenAI(prompt, "Sanitize value", config.getTimeoutMs());
    }

    @Override
    public String explainException(String exceptionType, String message, String stackTraceSnippet)
            throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Azure OpenAI service not healthy");
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

        return callAzureOpenAI(prompt, "Explain exception", config.getTimeoutMs());
    }

    @Override
    public String classifyData(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Azure OpenAI service not healthy");
        }

        String prompt = String.format(
            "Classify this data as one of: 'pii' (personally identifiable), 'sensitive' (confidential), 'public' (safe), or 'unknown'.\n" +
            "Return ONLY the classification.\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        String result = callAzureOpenAI(prompt, "Classify data", config.getTimeoutMs());
        return result.toLowerCase().trim();
    }

    @Override
    public Map<String, String> sanitizeBatch(List<String> values, List<String> contexts) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Azure OpenAI service not healthy");
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

        String response = callAzureOpenAI(prompt.toString(), "Batch sanitize", config.getTimeoutMs());

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
            throw new AIServiceException("Azure OpenAI service not healthy");
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

        String response = callAzureOpenAI(prompt.toString(), "Batch classify", config.getTimeoutMs());

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
            failed.completeExceptionally(new AIServiceException("Azure OpenAI service not healthy"));
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
                return makeAPICall(prompt);
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("API call failed: " + e.getMessage(), e));
            }
        }, executor).orTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletableFuture<String> explainExceptionAsync(String exceptionType, String message, String stackTraceSnippet) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Azure OpenAI service not healthy"));
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
                return makeAPICall(prompt);
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("API call failed: " + e.getMessage(), e));
            }
        }, executor).orTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletableFuture<String> classifyDataAsync(String value, String context) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Azure OpenAI service not healthy"));
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
                return makeAPICall(prompt);
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("API call failed: " + e.getMessage(), e));
            }
        }, executor).orTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
          .thenApply(String::toLowerCase);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public String getServiceName() {
        return String.format("Azure OpenAI (%s)", deploymentName);
    }

    /**
     * Call Azure OpenAI API with timeout protection.
     */
    private String callAzureOpenAI(String prompt, String taskName, long timeoutMs) throws AIServiceException {
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
     * Make actual HTTP call to Azure OpenAI API.
     */
    private String makeAPICall(String prompt) throws Exception {
        String urlString = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
            azureEndpoint, deploymentName, apiVersion);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Setup request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("api-key", config.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout((int) config.getTimeoutMs());
            conn.setReadTimeout((int) config.getTimeoutMs());

            // Build request body (same as OpenAI)
            JsonObject requestBody = new JsonObject();
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
     * Extract text content from Azure OpenAI API response (same as OpenAI).
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
     * Check if Azure OpenAI service is accessible.
     */
    private boolean checkHealth() {
        try {
            String result = callAzureOpenAI("Reply 'OK'", "Health check", 5000);
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            System.err.println("Azure OpenAI health check failed: " + e.getMessage());
            return false;
        }
    }
}