package com.logguardai.client;

import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;

/**
 * Factory for creating appropriate AIService implementations.
 */
public class AIServiceFactory {
    
    /**
     * Create an AI service based on configuration.
     * Returns NoOpAIService if not properly configured.
     */
    public static AIService createService(AIConfig config) {
        if (config == null || !config.isConfigured()) {
            return new NoOpAIService();
        }

        AIService service;
        
        if ("openai".equalsIgnoreCase(config.getApiProvider())) {
            service = new OpenAIService(config);
        } else if ("anthropic".equalsIgnoreCase(config.getApiProvider())) {
            service = new AnthropicAIService(config);
        } else if ("azure-openai".equalsIgnoreCase(config.getApiProvider())) {
            service = new AzureOpenAIService(config);
        } else {
            // Default to no-op for unknown providers
            service = new NoOpAIService();
        }

        // Wrap with caching if service is healthy
        if (service.isHealthy() && !(service instanceof NoOpAIService)) {
            service = new CachedAIService(service, 1000, 3600000);  // 1000 entries, 1 hour TTL
        }

        return service;
    }

    /**
     * Create a cached version of any AIService.
     */
    public static AIService withCaching(AIService service, int maxCacheSize, long cacheTTLMs) {
        if (service instanceof CachedAIService) {
            return service;  // Already cached
        }
        return new CachedAIService(service, maxCacheSize, cacheTTLMs);
    }
}
