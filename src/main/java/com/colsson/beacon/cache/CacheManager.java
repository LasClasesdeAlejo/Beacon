package com.colsson.beacon.cache;

import com.colsson.beacon.model.PermissionResult;
import com.colsson.beacon.model.PermissionState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché local por servidor para acelerar lecturas frecuentes.
 *
 * <p>Flujo de lectura:
 * <pre>
 * API → Cache → resultado
 *            ↓ (miss)
 *          MySQL → cache → resultado
 * </pre>
 *
 * <p>Flujo de escritura:
 * <pre>
 * Comando → Validación → MySQL → invalidación de cache → auditoría
 * </pre>
 *
 * <p>La caché se puede deshabilitar completamente.
 */
public class CacheManager {

    private final boolean enabled;
    private final long ttlMillis;
    private final int maxSize;

    private final Map<UUID, CacheEntry<Object>> userCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<Object>> groupCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<PermissionResult>> permissionCache = new ConcurrentHashMap<>();

    /**
     * @param enabled    si la caché está activa
     * @param ttlSeconds tiempo de vida de las entradas en segundos
     * @param maxSize    número máximo de entradas por categoría
     */
    public CacheManager(boolean enabled, int ttlSeconds, int maxSize) {
        this.enabled = enabled;
        this.ttlMillis = ttlSeconds * 1000L;
        this.maxSize = maxSize;
    }

    public CacheManager(boolean enabled, int ttlSeconds) {
        this(enabled, ttlSeconds, 1000);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ── Usuarios ────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getUser(UUID uuid) {
        if (!enabled) return Optional.empty();
        CacheEntry<Object> entry = userCache.get(uuid);
        if (entry == null || entry.isExpired(ttlMillis)) {
            if (entry != null) userCache.remove(uuid);
            return Optional.empty();
        }
        return Optional.of((T) entry.value());
    }

    public void putUser(UUID uuid, Object user) {
        if (!enabled) return;
        if (userCache.size() >= maxSize) {
            evictOldest(userCache);
        }
        userCache.put(uuid, new CacheEntry<>(user, System.currentTimeMillis()));
    }

    public void invalidateUser(UUID uuid) {
        userCache.remove(uuid);
    }

    // ── Grupos ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getGroup(String name) {
        if (!enabled) return Optional.empty();
        CacheEntry<Object> entry = groupCache.get(name);
        if (entry == null || entry.isExpired(ttlMillis)) {
            if (entry != null) groupCache.remove(name);
            return Optional.empty();
        }
        return Optional.of((T) entry.value());
    }

    public void putGroup(String name, Object group) {
        if (!enabled) return;
        if (groupCache.size() >= maxSize) {
            evictOldest(groupCache);
        }
        groupCache.put(name, new CacheEntry<>(group, System.currentTimeMillis()));
    }

    public void invalidateGroup(String name) {
        groupCache.remove(name);
    }

    // ── Permisos ────────────────────────────────────────────

    public Optional<PermissionResult> getPermissionResult(UUID userUuid, String permission) {
        return getPermissionResult(userUuid, permission, null);
    }

    public Optional<PermissionResult> getPermissionResult(UUID userUuid, String permission,
                                                           String world) {
        if (!enabled) return Optional.empty();
        String key = buildPermissionKey(userUuid, permission, world);
        CacheEntry<PermissionResult> entry = permissionCache.get(key);
        if (entry == null || entry.isExpired(ttlMillis)) {
            if (entry != null) permissionCache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public void putPermissionResult(UUID userUuid, String permission, PermissionResult result) {
        putPermissionResult(userUuid, permission, null, result);
    }

    public void putPermissionResult(UUID userUuid, String permission, String world,
                                     PermissionResult result) {
        if (!enabled) return;
        if (permissionCache.size() >= maxSize) {
            evictOldest(permissionCache);
        }
        permissionCache.put(buildPermissionKey(userUuid, permission, world),
                            new CacheEntry<>(result, System.currentTimeMillis()));
    }

    /**
     * Invalida todos los resultados de permisos de un usuario.
     */
    public void invalidateUserPermissions(UUID userUuid) {
        String prefix = userUuid + ":";
        permissionCache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Invalida todos los resultados de permisos que involucran un grupo dado.
     * Esto es necesario cuando se modifica un permiso de grupo.
     */
    public void invalidateGroupPermissions() {
        permissionCache.clear();
    }

    /**
     * Invalida toda la caché.
     */
    public void invalidateAll() {
        userCache.clear();
        groupCache.clear();
        permissionCache.clear();
    }

    // ── Estadísticas ────────────────────────────────────────

    public int userCacheSize() { return userCache.size(); }
    public int groupCacheSize() { return groupCache.size(); }
    public int permissionCacheSize() { return permissionCache.size(); }

    // ── Internos ────────────────────────────────────────────

    private String buildPermissionKey(UUID userUuid, String permission, String world) {
        return userUuid + ":" + permission + ":" + (world != null ? world : "global");
    }

    private <K, T> void evictOldest(Map<K, CacheEntry<T>> map) {
        if (map.isEmpty()) return;
        K oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (var entry : map.entrySet()) {
            if (entry.getValue().createdAt() < oldestTime) {
                oldestTime = entry.getValue().createdAt();
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            map.remove(oldestKey);
        }
    }
}
