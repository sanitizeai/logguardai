package com.logguardai.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public java.util.concurrent.CompletableFuture<String> sanitizeAsync(String value, String context) {
        String cacheKey = "sanitize:" + value + ":" + (context != null ? context : "");
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return java.util.concurrent.CompletableFuture.completedFuture(cached);
        }

        return delegate.sanitizeAsync(value, context).thenApply(result -> {
            cache.put(cacheKey, result);
            return result;
        });
    }

    @Override
    public java.util.concurrent.CompletableFuture<String> explainExceptionAsync(String exceptionType, String message, String stackTraceSnippet) {
        String cacheKey = "exception:" + exceptionType + ":" + message;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return java.util.concurrent.CompletableFuture.completedFuture(cached);
        }

        return delegate.explainExceptionAsync(exceptionType, message, stackTraceSnippet).thenApply(result -> {
            cache.put(cacheKey, result);
            return result;
        });
    }

    @Override
    public java.util.concurrent.CompletableFuture<String> classifyDataAsync(String value, String context) {
        String cacheKey = "classify:" + value + ":" + (context != null ? context : "");
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return java.util.concurrent.CompletableFuture.completedFuture(cached);
        }

        return delegate.classifyDataAsync(value, context).thenApply(result -> {
            cache.put(cacheKey, result);
            return result;
        });
    }

    @Override
    public Map<String, String> sanitizeBatch(List<String> values, List<String> contexts) throws AIServiceException {
        if (values == null || values.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();
        List<String> uncachedValues = new ArrayList<>();
        List<String> uncachedContexts = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();

        // Check cache for each value
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            String context = (contexts != null && i < contexts.size()) ? contexts.get(i) : "";
            String cacheKey = "sanitize:" + value + ":" + context;

            String cached = cache.get(cacheKey);
            if (cached != null) {
                result.put(value, cached);
            } else {
                uncachedValues.add(value);
                uncachedContexts.add(context);
                uncachedIndices.add(i);
            }
        }

        // Call delegate for uncached values
        if (!uncachedValues.isEmpty()) {
            Map<String, String> delegateResults = delegate.sanitizeBatch(uncachedValues, uncachedContexts);

            // Cache and add to result
            for (Map.Entry<String, String> entry : delegateResults.entrySet()) {
                String value = entry.getKey();
                String sanitized = entry.getValue();
                String context = uncachedContexts.get(uncachedValues.indexOf(value));
                String cacheKey = "sanitize:" + value + ":" + context;

                cache.put(cacheKey, sanitized);
                result.put(value, sanitized);
            }
        }

        return result;
    }

    @Override
    public Map<String, String> classifyDataBatch(List<String> values, List<String> contexts) throws AIServiceException {
        if (values == null || values.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();
        List<String> uncachedValues = new ArrayList<>();
        List<String> uncachedContexts = new ArrayList<>();

        // Check cache for each value
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            String context = (contexts != null && i < contexts.size()) ? contexts.get(i) : "";
            String cacheKey = "classify:" + value + ":" + context;

            String cached = cache.get(cacheKey);
            if (cached != null) {
                result.put(value, cached);
            } else {
                uncachedValues.add(value);
                uncachedContexts.add(context);
            }
        }

        // Call delegate for uncached values
        if (!uncachedValues.isEmpty()) {
            Map<String, String> delegateResults = delegate.classifyDataBatch(uncachedValues, uncachedContexts);

            // Cache and add to result
            for (Map.Entry<String, String> entry : delegateResults.entrySet()) {
                String value = entry.getKey();
                String sanitized = entry.getValue();
                String context = uncachedContexts.get(uncachedValues.indexOf(value));
                String cacheKey = "classify:" + value + ":" + context;

                cache.put(cacheKey, sanitized);
                result.put(value, sanitized);
            }
        }

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
