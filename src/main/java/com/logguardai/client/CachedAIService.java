package com.logguardai.client;

import com.logguardai.ai.AIService;
import com.logguardai.ai.AIServiceException;
import com.logguardai.cache.LRUCache;

/**
 * Cached wrapper for AIService implementations.
 * 
 * Features:
 * - Caches AI results to avoid repeated API calls
 * - LRU eviction with TTL support
 * - Falls through on cache misses
 * - Thread-safe caching
 */
public class CachedAIService implements AIService {
    
    private final AIService delegate;
    private final LRUCache<String, String> cache;
    private final int maxCacheSize;
    private final long cacheTTLMs;

    public CachedAIService(AIService delegate, int maxCacheSize, long cacheTTLMs) {
        this.delegate = delegate;
        this.maxCacheSize = maxCacheSize;
        this.cacheTTLMs = cacheTTLMs;
        this.cache = new LRUCache<>(maxCacheSize, cacheTTLMs);
    }

    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        String cacheKey = "sanitize:" + value + ":" + (context != null ? context : "");
        
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String result = delegate.sanitize(value, context);
        cache.put(cacheKey, result);
        return result;
    }

    @Override
    public String explainException(String exceptionType, String message, String stackTraceSnippet) 
            throws AIServiceException {
        String cacheKey = "exception:" + exceptionType + ":" + message;
        
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String result = delegate.explainException(exceptionType, message, stackTraceSnippet);
        cache.put(cacheKey, result);
        return result;
    }

    @Override
    public String classifyData(String value, String context) throws AIServiceException {
        String cacheKey = "classify:" + value + ":" + (context != null ? context : "");
        
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String result = delegate.classifyData(value, context);
        cache.put(cacheKey, result);
        return result;
    }

    @Override
    public boolean isHealthy() {
        return delegate.isHealthy();
    }

    @Override
    public String getServiceName() {
        return delegate.getServiceName() + " (cached: " + maxCacheSize + " entries)";
    }

    /**
     * Clear cache manually.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get cache statistics.
     */
    public LRUCache.CacheStats getCacheStats() {
        return cache.getStats();
    }
}
