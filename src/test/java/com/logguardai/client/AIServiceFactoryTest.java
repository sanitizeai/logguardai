package com.logguardai.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;

public class AIServiceFactoryTest {
    
    @Test
    public void testCreateNullConfig() {
        AIService service = AIServiceFactory.createService(null);
        assertNotNull(service);
        assertTrue(service.isHealthy());
    }
    
    @Test
    public void testCreateNotConfigured() {
        AIConfig config = new AIConfig();
        AIService service = AIServiceFactory.createService(config);
        assertNotNull(service);
    }
    
    @Test
    public void testCreateAnthropicConfig() {
        AIConfig config = new AIConfig();
        config.setApiProvider("anthropic");
        config.setApiKey("test-key");
        config.setModel("claude-3-sonnet-20240229");

        AIService service = AIServiceFactory.createService(config);
        assertNotNull(service);
        assertTrue(service instanceof AnthropicAIService);
    }

    @Test
    public void testCreateAzureOpenAIConfig() {
        AIConfig config = new AIConfig();
        config.setApiProvider("azure-openai");
        config.setApiKey("test-key");
        config.setModel("gpt-35-turbo");
        config.setAzureEndpoint("https://test.openai.azure.com");
        config.setAzureDeployment("gpt-35-turbo");

        AIService service = AIServiceFactory.createService(config);
        assertNotNull(service);
        assertTrue(service instanceof AzureOpenAIService);
    }

    @Test
    public void testCreateUnknownProvider() {
        AIConfig config = new AIConfig();
        config.setApiProvider("unknown-provider");
        config.setApiKey("test-key");

        AIService service = AIServiceFactory.createService(config);
        assertNotNull(service);
        assertTrue(service instanceof NoOpAIService);
    }
    
    @Test
    public void testCreateWithCaching() {
        AIService baseService = new NoOpAIService();
        AIService cachedService = AIServiceFactory.withCaching(baseService, 100, 3600000);
        
        assertNotNull(cachedService);
        assertTrue(cachedService instanceof CachedAIService);
    }
    
    @Test
    public void testCreateCachedTwice() {
        AIService baseService = new NoOpAIService();
        AIService cached1 = AIServiceFactory.withCaching(baseService, 100, 3600000);
        AIService cached2 = AIServiceFactory.withCaching(cached1, 100, 3600000);
        
        // Should return same instance, not double-wrap
        assertSame(cached1, cached2);
    }
}
