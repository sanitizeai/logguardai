package com.logguardai.client;

import com.logguardai.ai.AIConfig;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Tests for AzureOpenAIService.
 * Note: These tests use mock responses since we don't have real API keys in CI.
 */
public class AzureOpenAIServiceTest {

    @Test
    public void testServiceCreation() {
        AIConfig config = new AIConfig();
        config.setApiProvider("azure-openai");
        config.setApiKey("test-key");
        config.setModel("gpt-35-turbo");
        config.setAzureEndpoint("https://test.openai.azure.com");
        config.setAzureDeployment("gpt-35-turbo");
        config.setAzureApiVersion("2023-12-01");

        AzureOpenAIService service = new AzureOpenAIService(config);
        assertNotNull(service);
        assertEquals("Azure OpenAI (gpt-35-turbo)", service.getServiceName());
    }

    @Test
    public void testNoOpWhenNotConfigured() {
        AIConfig config = new AIConfig(); // No API key
        AzureOpenAIService service = new AzureOpenAIService(config);
        assertFalse(service.isHealthy());
    }

    @Test
    public void testSanitizeBatch() throws Exception {
        // Test with NoOpAIService since we can't test real API
        NoOpAIService noopService = new NoOpAIService();
        List<String> values = Arrays.asList("secret1", "secret2");
        List<String> contexts = Arrays.asList("password", "token");

        Map<String, String> result = noopService.sanitizeBatch(values, contexts);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("[REDACTED]", result.get("secret1"));
        assertEquals("[REDACTED]", result.get("secret2"));
    }

    @Test
    public void testClassifyDataBatch() throws Exception {
        NoOpAIService noopService = new NoOpAIService();
        List<String> values = Arrays.asList("user@example.com", "abcdef12345678901234567890123456");
        List<String> contexts = Arrays.asList("email", "apiKey");

        Map<String, String> result = noopService.classifyDataBatch(values, contexts);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("public", result.get("user@example.com"));
        assertEquals("sensitive", result.get("abcdef12345678901234567890123456"));
    }
}