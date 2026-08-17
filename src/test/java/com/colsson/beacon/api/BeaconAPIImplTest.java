package com.colsson.beacon.api;

import com.colsson.beacon.cache.CacheManager;
import com.colsson.beacon.model.*;
import com.colsson.beacon.persistence.*;
import com.colsson.beacon.resolver.PermissionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class BeaconAPIImplTest {

    private DatabaseManager db;
    private BeaconAPIImpl api;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDatabaseHelper.createInMemoryDb();
        TestDatabaseHelper.createSchema(db);

        api = new BeaconAPIImpl(
            db,
            new UserRepository(db),
            new GroupRepository(db),
            new PermissionRepository(db),
            new InheritanceRepository(db),
            new AuditRepository(db),
            new PermissionResolver(),
            new CacheManager(true, 300),
            Logger.getLogger("BeaconTest")
        );
        api.setServerName("test");
    }

    private UUID createUser(String name) {
        UUID uuid = UUID.randomUUID();
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("INSERT INTO users (uuid, username) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
        return uuid;
    }

    private long createGroupInDb(String name, int priority) {
        try {
            return new GroupRepository(db).create(name, priority, "");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ══════════════════════════════════════════════════════════
    // Consultas
    // ══════════════════════════════════════════════════════════

    @Nested
    class Queries {

        @Test
        void getUserReturnsEmptyForUnknown() {
            assertTrue(api.getUser(UUID.randomUUID()).isEmpty());
        }

        @Test
        void getUserReturnsUserWithGroupsAndPermissions() {
            UUID uuid = createUser("Colsson");
            long gid = createGroupInDb("VIP", 10);

            // Add user to group + direct permission
            api.addUserToGroup(uuid, "VIP", "admin", null);
            api.setUserPermission(uuid, "anvil.fly", true, "admin", null);

            Optional<User> user = api.getUser(uuid);
            assertTrue(user.isPresent());
            assertEquals("Colsson", user.get().username());
            assertEquals(1, user.get().groups().size());
            assertTrue(user.get().hasDirectPermission("anvil.fly"));
        }

        @Test
        void getGroupReturnsEmptyForUnknown() {
            assertTrue(api.getGroup("NonExistent").isEmpty());
        }

        @Test
        void getGroupReturnsGroupWithPermissions() {
            createGroupInDb("VIP", 10);
            api.setGroupPermission("VIP", "anvil.fly", true, "admin", null);

            Optional<Group> group = api.getGroup("VIP");
            assertTrue(group.isPresent());
            assertEquals("VIP", group.get().name());
            assertEquals(PermissionState.TRUE, group.get().getPermissionState("anvil.fly"));
        }

        @Test
        void getGroupsReturnsAll() {
            createGroupInDb("VIP", 10);
            createGroupInDb("MOD", 20);

            List<Group> groups = api.getGroups();
            assertEquals(2, groups.size());
        }

        @Test
        void findAllUsersReturnsAll() {
            createUser("Alice");
            createUser("Bob");

            List<User> users = api.findAllUsers();
            assertEquals(2, users.size());
        }

        @Test
        void hasPermissionReturnsTrue() {
            UUID uuid = createUser("Colsson");
            createGroupInDb("VIP", 10);
            api.addUserToGroup(uuid, "VIP", "admin", null);
            api.setGroupPermission("VIP", "anvil.fly", true, "admin", null);

            assertTrue(api.hasPermission(uuid, "anvil.fly"));
        }

        @Test
        void hasPermissionReturnsFalseForUndefined() {
            UUID uuid = createUser("Colsson");
            assertFalse(api.hasPermission(uuid, "anvil.fly"));
        }

        @Test
        void getPermissionStateReturnsCorrectResult() {
            UUID uuid = createUser("Colsson");
            createGroupInDb("VIP", 10);
            api.addUserToGroup(uuid, "VIP", "admin", null);
            api.setGroupPermission("VIP", "anvil.fly", true, "admin", null);

            PermissionResult result = api.getPermissionState(uuid, "anvil.fly");
            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("VIP", result.source());
        }

        @Test
        void getGroupsOfReturnsUserGroups() {
            UUID uuid = createUser("Colsson");
            createGroupInDb("VIP", 10);
            createGroupInDb("MOD", 20);
            api.addUserToGroup(uuid, "VIP", "admin", null);
            api.addUserToGroup(uuid, "MOD", "admin", null);

            List<Group> groups = api.getGroupsOf(uuid);
            assertEquals(2, groups.size());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Grupos — CRUD
    // ══════════════════════════════════════════════════════════

    @Nested
    class GroupCRUD {

        @Test
        void createGroupAndRetrieve() {
            long id = api.createGroup("VIP", 10, "Very Important", "admin", "test reason");
            assertTrue(id > 0);

            Optional<Group> group = api.getGroup("VIP");
            assertTrue(group.isPresent());
            assertEquals(10, group.get().priority());
            assertEquals("Very Important", group.get().description());
        }

        @Test
        void createDuplicateGroupThrows() {
            api.createGroup("VIP", 10, "", "admin", null);
            assertThrows(IllegalArgumentException.class,
                () -> api.createGroup("VIP", 20, "", "admin", null));
        }

        @Test
        void deleteGroupRemovesIt() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.deleteGroup("VIP", "admin", "cleanup");

            assertTrue(api.getGroup("VIP").isEmpty());
        }

        @Test
        void deleteNonExistentGroupThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> api.deleteGroup("Ghost", "admin", null));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Grupos — Edición
    // ══════════════════════════════════════════════════════════

    @Nested
    class GroupEdit {

        @Test
        void renameGroup() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.renameGroup("VIP", "VVIP", "admin", "rebrand");

            assertTrue(api.getGroup("VIP").isEmpty());
            assertTrue(api.getGroup("VVIP").isPresent());
        }

        @Test
        void renameToExistingNameThrows() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.createGroup("MOD", 20, "", "admin", null);

            assertThrows(IllegalArgumentException.class,
                () -> api.renameGroup("VIP", "MOD", "admin", null));
        }

        @Test
        void setGroupPriority() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.setGroupPriority("VIP", 99, "admin", "upgrade");

            Optional<Group> group = api.getGroup("VIP");
            assertTrue(group.isPresent());
            assertEquals(99, group.get().priority());
        }

        @Test
        void setGroupDescription() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.setGroupDescription("VIP", "Very Important Player", "admin", null);

            Optional<Group> group = api.getGroup("VIP");
            assertTrue(group.isPresent());
            assertEquals("Very Important Player", group.get().description());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Grupos — Herencia
    // ══════════════════════════════════════════════════════════

    @Nested
    class Inheritance {

        @Test
        void setParentAndRemove() {
            api.createGroup("MVP", 10, "", "admin", null);
            api.createGroup("MVP+", 20, "", "admin", null);

            api.setParent("MVP+", "MVP", "admin", "upgrade");

            List<Group> parents = api.getParents("MVP+");
            assertEquals(1, parents.size());
            assertEquals("MVP", parents.get(0).name());

            api.removeParent("MVP+", "MVP", "admin", null);
            assertTrue(api.getParents("MVP+").isEmpty());
        }

        @Test
        void setParentDetectsCycle() {
            api.createGroup("A", 10, "", "admin", null);
            api.createGroup("B", 20, "", "admin", null);

            api.setParent("A", "B", "admin", null);

            assertThrows(IllegalArgumentException.class,
                () -> api.setParent("B", "A", "admin", null));
        }

        @Test
        void getAncestorsReturnsAll() {
            api.createGroup("MVP", 10, "", "admin", null);
            api.createGroup("MVP+", 20, "", "admin", null);
            api.createGroup("MVP++", 30, "", "admin", null);

            api.setParent("MVP+", "MVP", "admin", null);
            api.setParent("MVP++", "MVP+", "admin", null);

            List<Group> ancestors = api.getAncestors("MVP++");
            assertEquals(2, ancestors.size());
        }

        @Test
        void duplicateParentThrows() {
            api.createGroup("A", 10, "", "admin", null);
            api.createGroup("B", 20, "", "admin", null);

            api.setParent("A", "B", "admin", null);
            assertThrows(IllegalArgumentException.class,
                () -> api.setParent("A", "B", "admin", null));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Permisos
    // ══════════════════════════════════════════════════════════

    @Nested
    class Permissions {

        @Test
        void groupPermissionSetAndRemove() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.setGroupPermission("VIP", "anvil.fly", true, "admin", null);

            Optional<Group> group = api.getGroup("VIP");
            assertTrue(group.isPresent());
            assertEquals(PermissionState.TRUE, group.get().getPermissionState("anvil.fly"));

            api.removeGroupPermission("VIP", "anvil.fly", "admin", null);

            group = api.getGroup("VIP");
            assertTrue(group.isPresent());
            assertEquals(PermissionState.UNDEFINED, group.get().getPermissionState("anvil.fly"));
        }

        @Test
        void userPermissionSetAndRemove() {
            UUID uuid = createUser("Colsson");
            api.setUserPermission(uuid, "anvil.fly", true, "admin", null);

            assertTrue(api.hasPermission(uuid, "anvil.fly"));

            api.removeUserPermission(uuid, "anvil.fly", "admin", null);

            assertFalse(api.hasPermission(uuid, "anvil.fly"));
        }

        @Test
        void userDirectPermissionOverridesGroup() {
            UUID uuid = createUser("Colsson");
            createGroupInDb("VIP", 10);
            api.addUserToGroup(uuid, "VIP", "admin", null);
            api.setGroupPermission("VIP", "anvil.fly", true, "admin", null);

            // Direct FALSE overrides group TRUE
            api.setUserPermission(uuid, "anvil.fly", false, "admin", "exception");

            PermissionResult result = api.getPermissionState(uuid, "anvil.fly");
            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("direct", result.source());
        }

        @Test
        void higherPriorityGroupWins() {
            UUID uuid = createUser("Colsson");
            createGroupInDb("LOW", 1);
            createGroupInDb("HIGH", 100);
            api.addUserToGroup(uuid, "LOW", "admin", null);
            api.addUserToGroup(uuid, "HIGH", "admin", null);

            api.setGroupPermission("LOW", "anvil.fly", true, "admin", null);
            api.setGroupPermission("HIGH", "anvil.fly", false, "admin", null);

            PermissionResult result = api.getPermissionState(uuid, "anvil.fly");
            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("HIGH", result.source());
        }

        @Test
        void clearGroupPermissions() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.setGroupPermission("VIP", "a", true, "admin", null);
            api.setGroupPermission("VIP", "b", false, "admin", null);

            api.clearGroupPermissions("VIP", "admin", null);

            Optional<Group> group = api.getGroup("VIP");
            assertTrue(group.isPresent());
            assertTrue(group.get().permissions().isEmpty());
        }

        @Test
        void clearUserPermissions() {
            UUID uuid = createUser("Colsson");
            api.setUserPermission(uuid, "a", true, "admin", null);
            api.setUserPermission(uuid, "b", false, "admin", null);

            api.clearUserPermissions(uuid, "admin", null);

            User user = api.getUser(uuid).get();
            assertTrue(user.directPermissions().isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Auditoría
    // ══════════════════════════════════════════════════════════

    @Nested
    class Audit {

        @Test
        void modificationsGenerateAuditEntries() {
            api.createGroup("VIP", 10, "test", "admin", "reason1");
            api.renameGroup("VIP", "VVIP", "admin", "reason2");
            api.deleteGroup("VVIP", "admin", "reason3");

            try {
                var auditRepo = new AuditRepository(db);
                var entries = auditRepo.findRecent(10);
                assertEquals(3, entries.size());
            } catch (Exception e) {
                fail(e);
            }
        }

        @Test
        void queriesDoNotGenerateAudit() {
            api.createGroup("VIP", 10, "", "admin", null);
            UUID uuid = createUser("Colsson");

            // Reset audit count
            try {
                var auditRepo = new AuditRepository(db);
                int before = auditRepo.findRecent(100).size();

                api.getGroup("VIP");
                api.getUser(uuid);
                api.hasPermission(uuid, "anvil.fly");
                api.getGroups();

                int after = auditRepo.findRecent(100).size();
                assertEquals(before, after);
            } catch (Exception e) {
                fail(e);
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // Cache
    // ══════════════════════════════════════════════════════════

    @Nested
    class CacheIntegration {

        @Test
        void secondGetUsesCache() {
            UUID uuid = createUser("Colsson");

            Optional<User> first = api.getUser(uuid);
            assertTrue(first.isPresent());
            assertEquals(0, first.get().groups().size());

            Optional<User> second = api.getUser(uuid);
            assertTrue(second.isPresent());
            assertEquals(uuid, second.get().uuid());
        }

        @Test
        void writeInvalidatesCache() {
            UUID uuid = createUser("Colsson");
            createGroupInDb("VIP", 10);

            api.getUser(uuid); // cached
            api.setUserPermission(uuid, "anvil.fly", true, "admin", null);

            // Should re-resolve from MySQL
            assertTrue(api.hasPermission(uuid, "anvil.fly"));
        }

        @Test
        void reloadClearsAllCache() {
            api.createGroup("VIP", 10, "", "admin", null);
            api.getGroup("VIP"); // cached

            api.reload();

            // Force re-read from MySQL
            Optional<Group> group = api.getGroup("VIP");
            assertTrue(group.isPresent());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Usuarios — Grupos
    // ══════════════════════════════════════════════════════════

    @Nested
    class UserGroups {

        @Test
        void addUserToGroupAndRemove() {
            UUID uuid = createUser("Colsson");
            createGroupInDb("VIP", 10);

            api.addUserToGroup(uuid, "VIP", "admin", null);
            assertEquals(1, api.getGroupsOf(uuid).size());

            api.removeUserFromGroup(uuid, "VIP", "admin", null);
            assertEquals(0, api.getGroupsOf(uuid).size());
        }

        @Test
        void addNonExistentUserThrows() {
            createGroupInDb("VIP", 10);
            assertThrows(IllegalArgumentException.class,
                () -> api.addUserToGroup(UUID.randomUUID(), "VIP", "admin", null));
        }

        @Test
        void addNonExistentGroupThrows() {
            UUID uuid = createUser("Colsson");
            assertThrows(IllegalArgumentException.class,
                () -> api.addUserToGroup(uuid, "Ghost", "admin", null));
        }
    }
}
