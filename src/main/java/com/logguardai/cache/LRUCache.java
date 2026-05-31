package com.logguardai.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe LRU (Least Recently Used) cache for caching AI results.
 * 
 * Features:
 * - Fixed capacity with LRU eviction
 * - TTL (time-to-live) support for cache entries
 * - Thread-safe read/write operations
 * - Simple get/put interface
 */
public class LRUCache<K, V> {
    
    private static class CacheEntry<V> {
        V value;
        long createdAt;
        long lastAccessedAt;
        
        CacheEntry(V value) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessedAt = createdAt;
        }
    }

    private final int maxCapacity;
    private final long ttlMs;
    private final LinkedHashMap<K, CacheEntry<V>> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Create LRU cache with given capacity and TTL.
     * 
     * @param maxCapacity Maximum number of entries (LRU evicts oldest)
     * @param ttlMs Time-to-live in milliseconds (0 = no expiration)
     */
    public LRUCache(int maxCapacity, long ttlMs) {
        this.maxCapacity = maxCapacity;
        this.ttlMs = ttlMs;
        this.cache = new LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > maxCapacity;
            }
        };
    }

    /**
     * Get value from cache (returns null if not found or expired).
     */
    public V get(K key) {
        if (key == null) {
            return null;
        }

        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null) {
                return null;
            }

            // Check if expired
            if (ttlMs > 0 && System.currentTimeMillis() - entry.createdAt > ttlMs) {
                // Expired, need to remove
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(key);
                    return null;
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            entry.lastAccessedAt = System.currentTimeMillis();
            return entry.value;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Put value into cache.
     */
    public void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            cache.put(key, new CacheEntry<>(value));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove entry from cache.
     */
    public void remove(K key) {
        if (key == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear all entries from cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get current cache size.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if cache contains key.
     */
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null) {
                return false;
            }

            // Check expiration
            if (ttlMs > 0 && System.currentTimeMillis() - entry.createdAt > ttlMs) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(key);
                    return false;
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            return true;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get statistics about cache usage.
     */
    public CacheStats getStats() {
        lock.readLock().lock();
        try {
            return new CacheStats(cache.size(), maxCapacity, ttlMs);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Cache statistics.
     */
    public static class CacheStats {
        public final int currentSize;
        public final int maxCapacity;
        public final long ttlMs;

        public CacheStats(int currentSize, int maxCapacity, long ttlMs) {
            this.currentSize = currentSize;
            this.maxCapacity = maxCapacity;
            this.ttlMs = ttlMs;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{size=%d/%d, ttl=%dms}", currentSize, maxCapacity, ttlMs);
        }
    }
}
