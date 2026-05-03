package com.logguardai.client;

import com.logguardai.ai.AIConfig;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Tests for AnthropicAIService.
 * Note: These tests use mock responses since we don't have real API keys in CI.
 */
public class AnthropicAIServiceTest {

    @Test
    public void testServiceCreation() {
        AIConfig config = new AIConfig();
        config.setApiProvider("anthropic");
        config.setApiKey("test-key");
        config.setModel("claude-3-sonnet-20240229");

        AnthropicAIService service = new AnthropicAIService(config);
        assertNotNull(service);
        assertEquals("Anthropic Claude (claude-3-sonnet-20240229)", service.getServiceName());
    }

    @Test
    public void testNoOpWhenNotConfigured() {
        AIConfig config = new AIConfig(); // No API key
        AnthropicAIService service = new AnthropicAIService(config);
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