package com.logguardai.client;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.logguardai.ai.AIService;
import com.logguardai.cache.LRUCache;

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
    
    @Test
    public void testBatchSanitizeCaching() throws Exception {
        AIService noop = new NoOpAIService();
        AIService cached = new CachedAIService(noop, 100, 3600000);
        
        List<String> values = Arrays.asList("value1", "value2", "value3");
        List<String> contexts = Arrays.asList("field1", "field2", "field3");
        
        // First batch call
        Map<String, String> result1 = cached.sanitizeBatch(values, contexts);
        
        // Same batch call should return cached results
        Map<String, String> result2 = cached.sanitizeBatch(values, contexts);
        
        assertEquals(result1, result2);
        assertEquals(3, result1.size());
        assertEquals("[REDACTED]", result1.get("value1"));
        assertEquals("[REDACTED]", result1.get("value2"));
        assertEquals("[REDACTED]", result1.get("value3"));
    }
    
    @Test
    public void testBatchClassifyCaching() throws Exception {
        AIService noop = new NoOpAIService();
        AIService cached = new CachedAIService(noop, 100, 3600000);
        
        // Use values that NoOpAIService classifies as sensitive (32+ hex chars or 40+ base64 chars)
        List<String> values = Arrays.asList(
            "abcdef12345678901234567890123456", // 32 hex chars = sensitive
            "normaltext", // short text = public
            "YWJjZGVmZ2hpams=YWJjZGVmZ2hpams=YWJjZGVmZ2hpams=" // 50+ base64 chars = sensitive
        );
        List<String> contexts = Arrays.asList("apiKey", "message", "secret");
        
        // First batch call
        Map<String, String> result1 = cached.classifyDataBatch(values, contexts);
        
        // Same batch call should return cached results
        Map<String, String> result2 = cached.classifyDataBatch(values, contexts);
        
        assertEquals(result1, result2);
        assertEquals(3, result1.size());
        assertEquals("sensitive", result1.get("abcdef12345678901234567890123456"));
        assertEquals("public", result1.get("normaltext"));
        assertEquals("sensitive", result1.get("YWJjZGVmZ2hpams=YWJjZGVmZ2hpams=YWJjZGVmZ2hpams="));
    }
    
    @Test
    public void testBatchPartialCaching() throws Exception {
        AIService noop = new NoOpAIService();
        AIService cached = new CachedAIService(noop, 100, 3600000);
        
        // First call with subset
        List<String> values1 = Arrays.asList("value1", "value2");
        List<String> contexts1 = Arrays.asList("field1", "field2");
        cached.sanitizeBatch(values1, contexts1);
        
        // Second call with overlapping values
        List<String> values2 = Arrays.asList("value2", "value3", "value4");
        List<String> contexts2 = Arrays.asList("field2", "field3", "field4");
        Map<String, String> result = cached.sanitizeBatch(values2, contexts2);
        
        // Should have results for all values
        assertEquals(3, result.size());
        assertEquals("[REDACTED]", result.get("value2")); // cached
        assertEquals("[REDACTED]", result.get("value3")); // new
        assertEquals("[REDACTED]", result.get("value4")); // new
    }
}
