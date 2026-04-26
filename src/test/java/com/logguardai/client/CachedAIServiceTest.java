package com.logguardai.client;

import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;
import com.logguardai.cache.LRUCache;
import org.junit.Test;
import static org.junit.Assert.*;

public class CachedAIServiceTest {
    
    @Test
    public void testCaching() throws Exception {
        // Create a NoOp service wrapped with caching
        AIService noop = new NoOpAIService();
        AIService cached = new CachedAIService(noop, 100, 3600000);
        
        // First call
        String result1 = cached.sanitize("value1", "field1");
        
        // Same call should return cached result
        String result2 = cached.sanitize("value1", "field1");
        
        assertEquals(result1, result2);
        assertEquals("[REDACTED]", result1);
    }
    
    @Test
    public void testCacheStats() throws Exception {
        AIService noop = new NoOpAIService();
        CachedAIService cached = new CachedAIService(noop, 100, 3600000);
        
        cached.sanitize("val1", "field1");
        cached.sanitize("val2", "field2");
        
        LRUCache.CacheStats stats = cached.getCacheStats();
        assertEquals(2, stats.currentSize);
        assertEquals(100, stats.maxCapacity);
    }
    
    @Test
    public void testClearCache() throws Exception {
        AIService noop = new NoOpAIService();
        CachedAIService cached = new CachedAIService(noop, 100, 3600000);
        
        cached.sanitize("val1", "field1");
        cached.clearCache();
        
        LRUCache.CacheStats stats = cached.getCacheStats();
        assertEquals(0, stats.currentSize);
    }
}
