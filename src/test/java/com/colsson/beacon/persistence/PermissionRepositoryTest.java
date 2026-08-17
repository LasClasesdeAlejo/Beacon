package com.colsson.beacon.persistence;

import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PermissionRepositoryTest {

    private DatabaseManager db;
    private PermissionRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDatabaseHelper.createInMemoryDb();
        TestDatabaseHelper.createSchema(db);
        repo = new PermissionRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    // ── Permisos de grupo ───────────────────────────────────

    @Test
    void setAndGetGroupPermission() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", true);

        var perms = repo.getGroupPermissions(groupId);
        assertEquals(1, perms.size());
        assertTrue(perms.get("anvil.fly"));
    }

    @Test
    void groupPermissionFalse() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", false);

        var perms = repo.getGroupPermissions(groupId);
        assertFalse(perms.get("anvil.fly"));
    }

    @Test
    void setGroupPermissionUpdatesExisting() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", true);
        repo.setGroupPermission(groupId, "anvil.fly", false);

        var perms = repo.getGroupPermissions(groupId);
        assertEquals(1, perms.size());
        assertFalse(perms.get("anvil.fly"));
    }

    @Test
    void removeGroupPermission() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");
        repo.setGroupPermission(groupId, "anvil.fly", true);

        boolean removed = repo.removeGroupPermission(groupId, "anvil.fly");

        assertTrue(removed);
        assertTrue(repo.getGroupPermissions(groupId).isEmpty());
    }

    @Test
    void removeGroupPermissionReturnsFalseWhenNotSet() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        assertFalse(repo.removeGroupPermission(groupId, "anvil.fly"));
    }

    @Test
    void clearGroupPermissions() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");
        repo.setGroupPermission(groupId, "anvil.fly", true);
        repo.setGroupPermission(groupId, "anvil.kick", false);

        repo.clearGroupPermissions(groupId);

        assertTrue(repo.getGroupPermissions(groupId).isEmpty());
    }

    @Test
    void multipleGroupPermissions() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");
        repo.setGroupPermission(groupId, "anvil.fly", true);
        repo.setGroupPermission(groupId, "anvil.kick", true);
        repo.setGroupPermission(groupId, "anvil.home", false);

        var perms = repo.getGroupPermissions(groupId);
        assertEquals(3, perms.size());
    }

    // ── Permisos de usuario ─────────────────────────────────

    @Test
    void setAndGetUserPermission() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");

        repo.setUserPermission(uuid, "anvil.fly", true);

        var perms = repo.getUserPermissions(uuid);
        assertEquals(1, perms.size());
        assertTrue(perms.get("anvil.fly"));
    }

    @Test
    void userPermissionFalse() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");

        repo.setUserPermission(uuid, "anvil.fly", false);

        var perms = repo.getUserPermissions(uuid);
        assertFalse(perms.get("anvil.fly"));
    }

    @Test
    void setUserPermissionUpdatesExisting() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");

        repo.setUserPermission(uuid, "anvil.fly", true);
        repo.setUserPermission(uuid, "anvil.fly", false);

        var perms = repo.getUserPermissions(uuid);
        assertEquals(1, perms.size());
        assertFalse(perms.get("anvil.fly"));
    }

    @Test
    void removeUserPermission() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");
        repo.setUserPermission(uuid, "anvil.fly", true);

        boolean removed = repo.removeUserPermission(uuid, "anvil.fly");

        assertTrue(removed);
        assertTrue(repo.getUserPermissions(uuid).isEmpty());
    }

    @Test
    void removeUserPermissionReturnsFalseWhenNotSet() throws Exception {
        UUID uuid = UUID.randomUUID();
        assertFalse(repo.removeUserPermission(uuid, "anvil.fly"));
    }

    @Test
    void clearUserPermissions() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");
        repo.setUserPermission(uuid, "anvil.fly", true);
        repo.setUserPermission(uuid, "anvil.kick", false);

        repo.clearUserPermissions(uuid);

        assertTrue(repo.getUserPermissions(uuid).isEmpty());
    }

    // ── Consultas globales ──────────────────────────────────

    @Test
    void findAllDistinctPermissions() throws Exception {
        var groupRepo = new GroupRepository(db);
        long g1 = groupRepo.create("A", 10, "");
        long g2 = groupRepo.create("B", 20, "");

        repo.setGroupPermission(g1, "anvil.fly", true);
        repo.setGroupPermission(g2, "anvil.fly", true);
        repo.setGroupPermission(g1, "anvil.kick", true);

        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "User1");
        repo.setUserPermission(uuid, "loom.chat.color", true);

        var all = repo.findAllDistinctPermissions();
        assertEquals(3, all.size());
        assertTrue(all.contains("anvil.fly"));
        assertTrue(all.contains("anvil.kick"));
        assertTrue(all.contains("loom.chat.color"));
    }

    // ── UNDEFINED behavior ──────────────────────────────────

    @Test
    void absenceRepresentsUndefined() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        // No permissions set → all UNDEFINED
        assertTrue(repo.getGroupPermissions(groupId).isEmpty());
    }

    @Test
    void removeRestoresUndefined() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");
        repo.setGroupPermission(groupId, "anvil.fly", false);

        // FALSE stored
        assertFalse(repo.getGroupPermissions(groupId).get("anvil.fly"));

        // Remove → UNDEFINED (absent)
        repo.removeGroupPermission(groupId, "anvil.fly");
        assertTrue(repo.getGroupPermissions(groupId).isEmpty());
    }

    // ── World-specific (V8 schema) ──────────────────────────

    @Test
    void worldColumnExists() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        // Verify world column exists by inserting with explicit world
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, groupId);
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 1);
            ps.setString(4, "lobby");
            ps.executeUpdate();
        }

        // Verify it can be read back
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT world FROM group_permissions WHERE group_id = ? AND permission = ?")) {
            ps.setLong(1, groupId);
            ps.setString(2, "anvil.fly");
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("lobby", rs.getString("world"));
            }
        }
    }

    @Test
    void globalAndWorldSpecificCoexist() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        // Insert global (world=NULL)
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, groupId);
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 1);
            ps.setNull(4, java.sql.Types.VARCHAR);
            ps.executeUpdate();
        }

        // Insert world-specific (world='skyblock') — same permission, different world
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, groupId);
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 0);
            ps.setString(4, "skyblock");
            ps.executeUpdate();
        }

        // Both should exist
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM group_permissions WHERE group_id = ?")) {
            ps.setLong(1, groupId);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }
        }
    }

    @Test
    void duplicateWorldSpecificRejected() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        // Insert world-specific
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, groupId);
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 1);
            ps.setString(4, "lobby");
            ps.executeUpdate();
        }

        // Duplicate same world should fail
        assertThrows(Exception.class, () -> {
            try (var conn = db.getConnection();
                 var ps = conn.prepareStatement(
                     "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, groupId);
                ps.setString(2, "anvil.fly");
                ps.setInt(3, 0);
                ps.setString(4, "lobby");
                ps.executeUpdate();
            }
        });
    }

    @Test
    void differentWorldSpecificsAllowed() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        // Insert for lobby
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, groupId);
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 1);
            ps.setString(4, "lobby");
            ps.executeUpdate();
        }

        // Insert for skyblock — same permission, different world → should succeed
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, groupId);
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 0);
            ps.setString(4, "skyblock");
            ps.executeUpdate();
        }

        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM group_permissions WHERE group_id = ?")) {
            ps.setLong(1, groupId);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }
        }
    }

    @Test
    void userWorldColumnExists() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");

        // Insert with world
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO user_permissions (user_uuid, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 1);
            ps.setString(4, "lobby");
            ps.executeUpdate();
        }

        // Verify both global and world-specific
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO user_permissions (user_uuid, permission, value, world) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, "anvil.fly");
            ps.setInt(3, 0);
            ps.setNull(4, java.sql.Types.VARCHAR);
            ps.executeUpdate();
        }

        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM user_permissions WHERE user_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }
        }
    }

    @Test
    void clearGroupPermissionsRemovesWorldSpecific() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        // Insert global + world-specific
        try (var conn = db.getConnection()) {
            try (var ps = conn.prepareStatement(
                     "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, groupId);
                ps.setString(2, "anvil.fly");
                ps.setInt(3, 1);
                ps.setNull(4, java.sql.Types.VARCHAR);
                ps.executeUpdate();
            }
            try (var ps = conn.prepareStatement(
                     "INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, groupId);
                ps.setString(2, "anvil.fly");
                ps.setInt(3, 0);
                ps.setString(4, "skyblock");
                ps.executeUpdate();
            }
        }

        // clearGroupPermissions should remove all
        repo.clearGroupPermissions(groupId);

        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM group_permissions WHERE group_id = ?")) {
            ps.setLong(1, groupId);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    // ── Phase 9.4: Repository methods ───────────────────────

    @Test
    void setGroupPermissionWithWorld() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", true, "lobby");
        repo.setGroupPermission(groupId, "anvil.fly", false, "skyblock");

        var byWorld = repo.getGroupPermissionsByWorld(groupId);
        assertEquals(2, byWorld.size());
        assertTrue(byWorld.get("lobby").get("anvil.fly"));
        assertFalse(byWorld.get("skyblock").get("anvil.fly"));
    }

    @Test
    void setGroupPermissionWorldDoesNotAffectGlobal() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", true);          // global
        repo.setGroupPermission(groupId, "anvil.fly", false, "lobby"); // world

        var byWorld = repo.getGroupPermissionsByWorld(groupId);
        assertEquals(2, byWorld.size());
        assertTrue(byWorld.get(null).get("anvil.fly"));   // global
        assertFalse(byWorld.get("lobby").get("anvil.fly")); // world
    }

    @Test
    void setGroupPermissionGlobalDoesNotAffectWorld() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", true);           // global
        repo.setGroupPermission(groupId, "anvil.fly", false, "lobby"); // world

        // Global set (old API) only affects global row
        var byWorld = repo.getGroupPermissionsByWorld(groupId);
        assertEquals(2, byWorld.size());
        assertTrue(byWorld.get(null).get("anvil.fly"));  // global
        assertFalse(byWorld.get("lobby").get("anvil.fly")); // world preserved
    }

    @Test
    void removeGroupPermissionWorldPreservesGlobal() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", true);          // global
        repo.setGroupPermission(groupId, "anvil.fly", false, "lobby"); // world

        repo.removeGroupPermission(groupId, "anvil.fly", "lobby");

        var byWorld = repo.getGroupPermissionsByWorld(groupId);
        assertEquals(1, byWorld.size());
        assertTrue(byWorld.get(null).get("anvil.fly"));
    }

    @Test
    void removeGroupPermissionGlobalPreservesWorld() throws Exception {
        var groupRepo = new GroupRepository(db);
        long groupId = groupRepo.create("VIP", 10, "");

        repo.setGroupPermission(groupId, "anvil.fly", true);          // global
        repo.setGroupPermission(groupId, "anvil.fly", false, "lobby"); // world

        repo.removeGroupPermission(groupId, "anvil.fly", null);

        var byWorld = repo.getGroupPermissionsByWorld(groupId);
        assertEquals(1, byWorld.size());
        assertFalse(byWorld.get("lobby").get("anvil.fly"));
    }

    @Test
    void setUserPermissionWithWorld() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");

        repo.setUserPermission(uuid, "anvil.fly", true, "lobby");
        repo.setUserPermission(uuid, "anvil.fly", false, "skyblock");

        var byWorld = repo.getUserPermissionsByWorld(uuid);
        assertEquals(2, byWorld.size());
        assertTrue(byWorld.get("lobby").get("anvil.fly"));
        assertFalse(byWorld.get("skyblock").get("anvil.fly"));
    }

    @Test
    void removeUserPermissionWorldPreservesGlobal() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");

        repo.setUserPermission(uuid, "anvil.fly", true);          // global
        repo.setUserPermission(uuid, "anvil.fly", false, "lobby"); // world

        repo.removeUserPermission(uuid, "anvil.fly", "lobby");

        var byWorld = repo.getUserPermissionsByWorld(uuid);
        assertEquals(1, byWorld.size());
        assertTrue(byWorld.get(null).get("anvil.fly"));
    }

    @Test
    void removeUserPermissionGlobalPreservesWorld() throws Exception {
        UUID uuid = UUID.randomUUID();
        var userRepo = new UserRepository(db);
        userRepo.create(uuid, "Colsson");

        repo.setUserPermission(uuid, "anvil.fly", true);          // global
        repo.setUserPermission(uuid, "anvil.fly", false, "lobby"); // world

        repo.removeUserPermission(uuid, "anvil.fly", null);

        var byWorld = repo.getUserPermissionsByWorld(uuid);
        assertEquals(1, byWorld.size());
        assertFalse(byWorld.get("lobby").get("anvil.fly"));
    }
}
