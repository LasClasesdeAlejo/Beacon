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
}
