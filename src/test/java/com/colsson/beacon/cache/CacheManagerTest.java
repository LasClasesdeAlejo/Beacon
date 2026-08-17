package com.colsson.beacon.cache;

import com.colsson.beacon.model.PermissionResult;
import com.colsson.beacon.model.PermissionState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerTest {

    // ══════════════════════════════════════════════════════════
    // Cache deshabilitada
    // ══════════════════════════════════════════════════════════

    @Nested
    class DisabledCache {

        @Test
        void disabledCacheReturnsEmpty() {
            CacheManager cache = new CacheManager(false, 300);
            UUID uuid = UUID.randomUUID();

            assertFalse(cache.isEnabled());
            assertTrue(cache.getUser(uuid).isEmpty());
        }

        @Test
        void disabledCacheDoesNotStore() {
            CacheManager cache = new CacheManager(false, 300);
            UUID uuid = UUID.randomUUID();

            cache.putUser(uuid, "data");

            assertTrue(cache.getUser(uuid).isEmpty());
            assertEquals(0, cache.userCacheSize());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Cache habilitada — Usuarios
    // ══════════════════════════════════════════════════════════

    @Nested
    class UserCache {

        @Test
        void putAndGetUser() {
            CacheManager cache = new CacheManager(true, 300);
            UUID uuid = UUID.randomUUID();

            cache.putUser(uuid, "Colsson");

            Optional<String> result = cache.getUser(uuid);
            assertTrue(result.isPresent());
            assertEquals("Colsson", result.get());
        }

        @Test
        void getUserReturnsEmptyOnMiss() {
            CacheManager cache = new CacheManager(true, 300);

            assertTrue(cache.getUser(UUID.randomUUID()).isEmpty());
        }

        @Test
        void invalidateUser() {
            CacheManager cache = new CacheManager(true, 300);
            UUID uuid = UUID.randomUUID();

            cache.putUser(uuid, "Colsson");
            cache.invalidateUser(uuid);

            assertTrue(cache.getUser(uuid).isEmpty());
        }

        @Test
        void userCacheSize() {
            CacheManager cache = new CacheManager(true, 300);

            cache.putUser(UUID.randomUUID(), "A");
            cache.putUser(UUID.randomUUID(), "B");

            assertEquals(2, cache.userCacheSize());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Cache habilitada — Grupos
    // ══════════════════════════════════════════════════════════

    @Nested
    class GroupCache {

        @Test
        void putAndGetGroup() {
            CacheManager cache = new CacheManager(true, 300);

            cache.putGroup("VIP", "group-object");

            Optional<Object> result = cache.getGroup("VIP");
            assertTrue(result.isPresent());
            assertEquals("group-object", result.get());
        }

        @Test
        void getGroupReturnsEmptyOnMiss() {
            CacheManager cache = new CacheManager(true, 300);

            assertTrue(cache.getGroup("NonExistent").isEmpty());
        }

        @Test
        void invalidateGroup() {
            CacheManager cache = new CacheManager(true, 300);

            cache.putGroup("VIP", "data");
            cache.invalidateGroup("VIP");

            assertTrue(cache.getGroup("VIP").isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Cache habilitada — Permisos
    // ══════════════════════════════════════════════════════════

    @Nested
    class PermissionCache {

        @Test
        void putAndGetPermissionResult() {
            CacheManager cache = new CacheManager(true, 300);
            UUID uuid = UUID.randomUUID();
            PermissionResult result = new PermissionResult(
                PermissionState.TRUE, "VIP", "trace");

            cache.putPermissionResult(uuid, "anvil.fly", result);

            Optional<PermissionResult> cached = cache.getPermissionResult(uuid, "anvil.fly");
            assertTrue(cached.isPresent());
            assertEquals(PermissionState.TRUE, cached.get().state());
            assertEquals("VIP", cached.get().source());
        }

        @Test
        void getPermissionReturnsEmptyOnMiss() {
            CacheManager cache = new CacheManager(true, 300);

            assertTrue(cache.getPermissionResult(UUID.randomUUID(), "anvil.fly").isEmpty());
        }

        @Test
        void invalidateUserPermissions() {
            CacheManager cache = new CacheManager(true, 300);
            UUID uuid = UUID.randomUUID();

            cache.putPermissionResult(uuid, "anvil.fly",
                new PermissionResult(PermissionState.TRUE, "VIP", "trace"));
            cache.putPermissionResult(uuid, "anvil.kick",
                new PermissionResult(PermissionState.FALSE, "MOD", "trace"));

            cache.invalidateUserPermissions(uuid);

            assertTrue(cache.getPermissionResult(uuid, "anvil.fly").isEmpty());
            assertTrue(cache.getPermissionResult(uuid, "anvil.kick").isEmpty());
        }

        @Test
        void invalidateUserPermissionsOnlyAffectsThatUser() {
            CacheManager cache = new CacheManager(true, 300);
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            cache.putPermissionResult(user1, "anvil.fly",
                new PermissionResult(PermissionState.TRUE, "VIP", "trace"));
            cache.putPermissionResult(user2, "anvil.fly",
                new PermissionResult(PermissionState.FALSE, "MOD", "trace"));

            cache.invalidateUserPermissions(user1);

            assertTrue(cache.getPermissionResult(user1, "anvil.fly").isEmpty());
            assertFalse(cache.getPermissionResult(user2, "anvil.fly").isEmpty());
        }

        @Test
        void invalidateGroupPermissionsClearsAll() {
            CacheManager cache = new CacheManager(true, 300);
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            cache.putPermissionResult(user1, "anvil.fly",
                new PermissionResult(PermissionState.TRUE, "VIP", "trace"));
            cache.putPermissionResult(user2, "anvil.fly",
                new PermissionResult(PermissionState.FALSE, "MOD", "trace"));

            cache.invalidateGroupPermissions();

            assertTrue(cache.getPermissionResult(user1, "anvil.fly").isEmpty());
            assertTrue(cache.getPermissionResult(user2, "anvil.fly").isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════
    // TTL (Time To Live)
    // ══════════════════════════════════════════════════════════

    @Nested
    class TTL {

        @Test
        void entryExpiresAfterTtl() throws InterruptedException {
            CacheManager cache = new CacheManager(true, 1);
            UUID uuid = UUID.randomUUID();

            cache.putUser(uuid, "Colsson");

            // Before TTL
            assertTrue(cache.getUser(uuid).isPresent());

            // Wait for expiry
            Thread.sleep(1100);

            // After TTL
            assertTrue(cache.getUser(uuid).isEmpty());
        }

        @Test
        void groupExpiresAfterTtl() throws InterruptedException {
            CacheManager cache = new CacheManager(true, 1);

            cache.putGroup("VIP", "data");

            assertTrue(cache.getGroup("VIP").isPresent());

            Thread.sleep(1100);

            assertTrue(cache.getGroup("VIP").isEmpty());
        }

        @Test
        void permissionExpiresAfterTtl() throws InterruptedException {
            CacheManager cache = new CacheManager(true, 1);
            UUID uuid = UUID.randomUUID();

            cache.putPermissionResult(uuid, "anvil.fly",
                new PermissionResult(PermissionState.TRUE, "VIP", "trace"));

            assertTrue(cache.getPermissionResult(uuid, "anvil.fly").isPresent());

            Thread.sleep(1100);

            assertTrue(cache.getPermissionResult(uuid, "anvil.fly").isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════
    // InvalidateAll
    // ══════════════════════════════════════════════════════════

    @Nested
    class InvalidateAll {

        @Test
        void clearsEverything() {
            CacheManager cache = new CacheManager(true, 300);
            UUID uuid = UUID.randomUUID();

            cache.putUser(uuid, "Colsson");
            cache.putGroup("VIP", "data");
            cache.putPermissionResult(uuid, "anvil.fly",
                new PermissionResult(PermissionState.TRUE, "VIP", "trace"));

            cache.invalidateAll();

            assertEquals(0, cache.userCacheSize());
            assertEquals(0, cache.groupCacheSize());
            assertEquals(0, cache.permissionCacheSize());
        }
    }

    // ══════════════════════════════════════════════════════════
    // MaxSize eviction
    // ══════════════════════════════════════════════════════════

    @Nested
    class MaxSize {

        @Test
        void evictsOldestWhenFull() {
            CacheManager cache = new CacheManager(true, 300, 3);

            cache.putUser(UUID.randomUUID(), "A");
            cache.putUser(UUID.randomUUID(), "B");
            cache.putUser(UUID.randomUUID(), "C");

            assertEquals(3, cache.userCacheSize());

            // This should evict the oldest
            cache.putUser(UUID.randomUUID(), "D");

            assertEquals(3, cache.userCacheSize());
        }
    }
}
