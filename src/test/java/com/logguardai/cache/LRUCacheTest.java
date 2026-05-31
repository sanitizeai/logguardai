package com.logguardai.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LRUCacheTest {
    
    @Test
    public void testPutAndGet() {
        LRUCache<String, String> cache = new LRUCache<>(10, 0);
        cache.put("key1", "value1");
        
        String result = cache.get("key1");
        assertEquals("value1", result);
    }
    
    @Test
    public void testGetMissing() {
        LRUCache<String, String> cache = new LRUCache<>(10, 0);
        
        String result = cache.get("nonexistent");
        assertNull(result);
    }
    
    @Test
    public void testPutNull() {
        LRUCache<String, String> cache = new LRUCache<>(10, 0);
        cache.put(null, "value");
        cache.put("key", null);
        
        assertNull(cache.get(null));
        assertNull(cache.get("key"));
    }
    
    @Test
    public void testLRUEviction() {
        LRUCache<String, String> cache = new LRUCache<>(3, 0);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        
        assertEquals(3, cache.size());
        
        // Add 4th item, should evict LRU
        cache.put("key4", "value4");
        assertEquals(3, cache.size());
        
        // key1 should be evicted
        assertNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        assertNotNull(cache.get("key3"));
        assertNotNull(cache.get("key4"));
    }
    
    @Test
    public void testContainsKey() {
        LRUCache<String, String> cache = new LRUCache<>(10, 0);
        cache.put("key1", "value1");
        
        assertTrue(cache.containsKey("key1"));
        assertFalse(cache.containsKey("key2"));
    }
    
    @Test
    public void testRemove() {
        LRUCache<String, String> cache = new LRUCache<>(10, 0);
        cache.put("key1", "value1");
        cache.remove("key1");
        
        assertNull(cache.get("key1"));
        assertEquals(0, cache.size());
    }
    
    @Test
    public void testClear() {
        LRUCache<String, String> cache = new LRUCache<>(10, 0);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();
        
        assertEquals(0, cache.size());
    }
    
    @Test
    public void testGetStats() {
        LRUCache<String, String> cache = new LRUCache<>(10, 5000);
        cache.put("key1", "value1");
        
        LRUCache.CacheStats stats = cache.getStats();
        assertEquals(1, stats.currentSize);
        assertEquals(10, stats.maxCapacity);
        assertEquals(5000, stats.ttlMs);
    }
    
    @Test
    public void testThreadSafety() throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(100, 0);
        
        // Multiple threads putting and getting
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                cache.put("key" + i, "value" + i);
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                cache.get("key" + i);
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        assertTrue(cache.size() > 0);
    }
}
