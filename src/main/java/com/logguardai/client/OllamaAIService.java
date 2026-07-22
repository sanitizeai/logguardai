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
 * Ollama-based AI service implementation.
 * Queries a local Ollama instance running on localhost or a configured network endpoint.
 */
public class OllamaAIService implements AIService {

    private final AIConfig config;
    private boolean healthy = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public OllamaAIService(AIConfig config) {
        this.config = config;
        this.healthy = checkHealth();
    }

    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Ollama service not healthy or unreachable");
        }

        String prompt = String.format(
            "You are a data sanitization expert. Sanitize this sensitive value without revealing its type or content. " +
            "Return ONLY a sanitized version (e.g., '[REDACTED]', '[MASKED]', a hash-like string, or generic placeholder).\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        return callOllama(prompt, "Sanitize value", config.getTimeoutMs());
    }

    @Override
    public String explainException(String exceptionType, String message, String stackTraceSnippet) 
            throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Ollama service not healthy or unreachable");
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

        return callOllama(prompt, "Explain exception", config.getTimeoutMs());
    }

    @Override
    public String classifyData(String value, String context) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Ollama service not healthy or unreachable");
        }

        String prompt = String.format(
            "Classify this data as one of: 'pii' (personally identifiable), 'sensitive' (confidential), 'public' (safe), or 'unknown'.\n" +
            "Return ONLY the classification.\n\n" +
            "Field: %s\nValue: %s",
            context != null ? context : "unknown", value
        );

        String result = callOllama(prompt, "Classify data", config.getTimeoutMs());
        return result.toLowerCase().trim();
    }

    @Override
    public CompletableFuture<String> sanitizeAsync(String value, String context) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Ollama service not healthy"));
            return failed;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return sanitize(value, context);
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("Ollama call failed: " + e.getMessage(), e));
            }
        }, executor);
    }

    @Override
    public CompletableFuture<String> explainExceptionAsync(String exceptionType, String message, String stackTraceSnippet) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Ollama service not healthy"));
            return failed;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return explainException(exceptionType, message, stackTraceSnippet);
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("Ollama call failed: " + e.getMessage(), e));
            }
        }, executor);
    }

    @Override
    public CompletableFuture<String> classifyDataAsync(String value, String context) {
        if (!healthy) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new AIServiceException("Ollama service not healthy"));
            return failed;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return classifyData(value, context);
            } catch (Exception e) {
                throw new CompletionException(new AIServiceException("Ollama call failed: " + e.getMessage(), e));
            }
        }, executor).thenApply(String::toLowerCase);
    }

    @Override
    public Map<String, String> sanitizeBatch(List<String> values, List<String> contexts) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Ollama service not healthy");
        }

        if (values == null || values.isEmpty()) {
            return new HashMap<>();
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a data sanitization expert. Sanitize these sensitive values without revealing their type or content.\n");
        prompt.append("Return ONLY a valid JSON object where each key is the original value and the value is the sanitized version.\n");
        prompt.append("Use generic placeholders like '[REDACTED]', '[MASKED]', hash-like strings, etc.\n\n");

        for (int i = 0; i < values.size(); i++) {
            String context = (contexts != null && i < contexts.size()) ? contexts.get(i) : "unknown";
            prompt.append(String.format("Field %d: %s\nValue %d: %s\n\n", i + 1, context, i + 1, values.get(i)));
        }

        prompt.append("Return format: {\"original_value1\": \"sanitized1\", \"original_value2\": \"sanitized2\", ...}");

        String response = callOllama(prompt.toString(), "Batch sanitize", config.getTimeoutMs());

        try {
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return result;
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse batch sanitization response from Ollama: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> classifyDataBatch(List<String> values, List<String> contexts) throws AIServiceException {
        if (!healthy) {
            throw new AIServiceException("Ollama service not healthy");
        }

        if (values == null || values.isEmpty()) {
            return new HashMap<>();
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Classify these data values as one of: 'pii' (personally identifiable), 'sensitive' (confidential), 'public' (safe), or 'unknown'.\n");
        prompt.append("Return ONLY a valid JSON object where each key is the original value and the value is the classification.\n\n");

        for (int i = 0; i < values.size(); i++) {
            String context = (contexts != null && i < contexts.size()) ? contexts.get(i) : "unknown";
            prompt.append(String.format("Field %d: %s\nValue %d: %s\n\n", i + 1, context, i + 1, values.get(i)));
        }

        prompt.append("Return format: {\"value1\": \"pii\", \"value2\": \"sensitive\", ...}");

        String response = callOllama(prompt.toString(), "Batch classify", config.getTimeoutMs());

        try {
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString().toLowerCase().trim());
            }
            return result;
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse batch classification response from Ollama: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public String getServiceName() {
        return String.format("Ollama (endpoint: %s, model: %s)", config.getOllamaEndpoint(), config.getModel());
    }

    private String callOllama(String prompt, String taskName, long timeoutMs) throws AIServiceException {
        try {
            Future<String> future = executor.submit(() -> {
                try {
                    return makeAPICall(prompt);
                } catch (Exception e) {
                    throw new AIServiceException("Ollama API call failed: " + e.getMessage(), e);
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

    protected String makeAPICall(String prompt) throws Exception {
        String endpoint = config.getOllamaEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = "http://localhost:11434";
        }
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        URL url = new URL(endpoint + "/api/chat");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout((int) config.getTimeoutMs());
            conn.setReadTimeout((int) config.getTimeoutMs());

            // Build Ollama request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
            requestBody.addProperty("stream", false);

            JsonObject options = new JsonObject();
            options.addProperty("temperature", config.getTemperature());
            options.addProperty("num_predict", config.getMaxTokens());
            requestBody.add("options", options);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);

            requestBody.add("messages", messages);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                String error = readStream(conn.getErrorStream());
                throw new Exception("Ollama error " + status + ": " + error);
            }

            String responseStr = readStream(conn.getInputStream());
            JsonObject json = new JsonParser().parse(responseStr).getAsJsonObject();
            JsonObject messageObj = json.getAsJsonObject("message");
            if (messageObj == null) {
                throw new Exception("Invalid response structure from Ollama");
            }
            return messageObj.get("content").getAsString().trim();

        } finally {
            conn.disconnect();
        }
    }

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

    private boolean checkHealth() {
        try {
            String endpoint = config.getOllamaEndpoint();
            if (endpoint == null || endpoint.isEmpty()) {
                endpoint = "http://localhost:11434";
            }
            if (endpoint.endsWith("/")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }
            // A simple GET request to base URL or /api/tags
            URL url = new URL(endpoint + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            int status = conn.getResponseCode();
            conn.disconnect();
            return status == 200;
        } catch (Exception e) {
            // Health check failed
            return false;
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
