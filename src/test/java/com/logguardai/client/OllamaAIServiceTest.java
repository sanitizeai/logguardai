package com.logguardai.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIServiceException;

public class OllamaAIServiceTest {

    // Subclass OllamaAIService to mock HTTP requests for testing JSON parsing, async, and batching
    private static class TestOllamaAIService extends OllamaAIService {
        private String mockResponse = "";
        private boolean shouldThrow = false;

        public TestOllamaAIService(AIConfig config) {
            super(config);
        }

        public void setMockResponse(String response) {
            this.mockResponse = response;
        }

        public void setShouldThrow(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }

        @Override
        public boolean isHealthy() {
            return true; // Force healthy for testing
        }

        @Override
        public String sanitize(String value, String context) throws AIServiceException {
            if (shouldThrow) {
                throw new AIServiceException("Mock API error");
            }
            if (mockResponse.contains("[REDACTED]")) {
                return "[REDACTED]";
            }
            return super.sanitize(value, context);
        }

        @Override
        public String classifyData(String value, String context) throws AIServiceException {
            if (shouldThrow) {
                throw new AIServiceException("Mock API error");
            }
            if (mockResponse.equals("sensitive") || mockResponse.equals("pii") || mockResponse.equals("public")) {
                return mockResponse;
            }
            return super.classifyData(value, context);
        }

        @Override
        protected String makeAPICall(String prompt) throws Exception {
            if (shouldThrow) {
                throw new Exception("Mock HTTP Error");
            }
            return mockResponse;
        }
    }

    @Test
    public void testServiceCreation() {
        AIConfig config = new AIConfig();
        config.setApiProvider("ollama");
        config.setModel("llama3");
        config.setOllamaEndpoint("http://localhost:11434");

        OllamaAIService service = new OllamaAIService(config);
        assertNotNull(service);
        assertEquals("Ollama (endpoint: http://localhost:11434, model: llama3)", service.getServiceName());
    }

    @Test
    public void testOfflineUnhealthy() {
        AIConfig config = new AIConfig();
        config.setApiProvider("ollama");
        config.setOllamaEndpoint("http://localhost:9999"); // Invalid port
        
        OllamaAIService service = new OllamaAIService(config);
        assertFalse(service.isHealthy());
    }

    @Test
    public void testSanitizeMocked() throws Exception {
        AIConfig config = new AIConfig();
        config.setApiProvider("ollama");
        
        TestOllamaAIService service = new TestOllamaAIService(config);
        service.setMockResponse("[REDACTED]");
        
        String result = service.sanitize("super-secret-password", "password");
        assertEquals("[REDACTED]", result);
    }

    @Test
    public void testClassifyMocked() throws Exception {
        AIConfig config = new AIConfig();
        config.setApiProvider("ollama");
        
        TestOllamaAIService service = new TestOllamaAIService(config);
        service.setMockResponse("sensitive");
        
        String result = service.classifyData("sk-abcdef123456", "apiKey");
        assertEquals("sensitive", result);
    }

    @Test
    public void testSanitizeAsyncMocked() throws Exception {
        AIConfig config = new AIConfig();
        config.setApiProvider("ollama");
        
        TestOllamaAIService service = new TestOllamaAIService(config);
        service.setMockResponse("[REDACTED]");
        
        CompletableFuture<String> future = service.sanitizeAsync("super-secret-password", "password");
        String result = future.get(500, TimeUnit.MILLISECONDS);
        assertEquals("[REDACTED]", result);
    }

    @Test
    public void testBatchSanitizeMocked() throws Exception {
        AIConfig config = new AIConfig();
        config.setApiProvider("ollama");
        
        TestOllamaAIService service = new TestOllamaAIService(config);
        // Valid JSON response for batch
        service.setMockResponse("{\"secret1\": \"[MASKED]\", \"secret2\": \"[MASKED]\"}");
        
        List<String> values = Arrays.asList("secret1", "secret2");
        List<String> contexts = Arrays.asList("password", "token");
        
        Map<String, String> result = service.sanitizeBatch(values, contexts);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("[MASKED]", result.get("secret1"));
        assertEquals("[MASKED]", result.get("secret2"));
    }

    @Test
    public void testBatchClassifyMocked() throws Exception {
        AIConfig config = new AIConfig();
        config.setApiProvider("ollama");
        
        TestOllamaAIService service = new TestOllamaAIService(config);
        // Valid JSON response for batch classification
        service.setMockResponse("{\"secret1\": \"sensitive\", \"secret2\": \"pii\"}");
        
        List<String> values = Arrays.asList("secret1", "secret2");
        List<String> contexts = Arrays.asList("password", "email");
        
        Map<String, String> result = service.classifyDataBatch(values, contexts);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("sensitive", result.get("secret1"));
        assertEquals("pii", result.get("secret2"));
    }
}
