package com.colsson.beacon.cache;

/**
 * Entrada de caché con timestamp para TTL.
 */
record CacheEntry<T>(T value, long createdAt) {

    boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - createdAt > ttlMillis;
    }
}
