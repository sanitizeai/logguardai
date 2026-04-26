package com.logguardai.client;

import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;
import org.junit.Test;
import static org.junit.Assert.*;

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
    public void testCreateOpenAIConfig() {
        AIConfig config = new AIConfig();
        config.setApiProvider("openai");
        config.setApiKey("test-key");  // Would fail health check, but doesn't throw
        
        AIService service = AIServiceFactory.createService(config);
        assertNotNull(service);
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
