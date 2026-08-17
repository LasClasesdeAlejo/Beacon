package com.colsson.beacon.commands;

import com.colsson.beacon.api.BeaconAPIImpl;
import com.colsson.beacon.cache.CacheManager;
import com.colsson.beacon.commands.handlers.*;
import com.colsson.beacon.model.*;
import com.colsson.beacon.persistence.*;
import com.colsson.beacon.resolver.PermissionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CommandRouterTest {

    private DatabaseManager db;
    private BeaconAPIImpl api;
    private BeaconCommandRouter router;
    private List<String> responses;
    private CommandContext ctx;

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

        AuditRepository auditRepo = new AuditRepository(db);
        router = new BeaconCommandRouter(api, auditRepo, Logger.getLogger("BeaconTest"));

        responses = new ArrayList<>();
        ctx = new CommandContext("TestPlayer", new String[]{}, responses::add, true, UUID.randomUUID().toString());
    }

    private void run(String... args) {
        router.dispatch(ctx, args);
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

    // ══════════════════════════════════════════════════════════
    // Help
    // ══════════════════════════════════════════════════════════

    @Nested
    class Help {

        @Test
        void helpShowsUsage() {
            run("help");
            assertFalse(responses.isEmpty());
            assertTrue(responses.get(0).contains("Ayuda"));
        }

        @Test
        void noArgsShowsUsage() {
            run();
            assertFalse(responses.isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Info
    // ══════════════════════════════════════════════════════════

    @Nested
    class Info {

        @Test
        void infoShowsPluginInfo() {
            run("info");
            assertFalse(responses.isEmpty());
            assertTrue(responses.stream().anyMatch(s -> s.contains("Beacon")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Reload
    // ══════════════════════════════════════════════════════════

    @Nested
    class Reload {

        @Test
        void reloadClearsCache() {
            run("reload");
            assertTrue(responses.stream().anyMatch(s -> s.contains("recargado")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // User commands
    // ══════════════════════════════════════════════════════════

    @Nested
    class UserCommands {

        @Test
        void userInfoShowsUserData() {
            UUID uuid = createUser("Colsson");
            run("user", uuid.toString(), "info");
            assertTrue(responses.stream().anyMatch(s -> s.contains("Colsson")));
        }

        @Test
        void userGroupsShowsGroups() {
            UUID uuid = createUser("Colsson");
            createGroupDb("VIP", 10);
            api.addUserToGroup(uuid, "VIP", "admin", null);

            run("user", uuid.toString(), "groups");
            assertTrue(responses.stream().anyMatch(s -> s.contains("VIP")));
        }

        @Test
        void userGroupAddAddsUser() {
            UUID uuid = createUser("Colsson");
            createGroupDb("VIP", 10);

            run("user", uuid.toString(), "group", "add", "VIP");
            assertTrue(responses.stream().anyMatch(s -> s.contains("añadido")));
            assertEquals(1, api.getGroupsOf(uuid).size());
        }

        @Test
        void userGroupRemoveRemovesUser() {
            UUID uuid = createUser("Colsson");
            createGroupDb("VIP", 10);
            api.addUserToGroup(uuid, "VIP", "admin", null);

            run("user", uuid.toString(), "group", "remove", "VIP");
            assertTrue(responses.stream().anyMatch(s -> s.contains("eliminado")));
            assertEquals(0, api.getGroupsOf(uuid).size());
        }

        @Test
        void userPermissionSetWorks() {
            UUID uuid = createUser("Colsson");

            run("user", uuid.toString(), "permission", "set", "anvil.fly", "true");
            assertTrue(responses.stream().anyMatch(s -> s.contains("Permiso")));
            assertTrue(api.hasPermission(uuid, "anvil.fly"));
        }

        @Test
        void userPermissionRemoveWorks() {
            UUID uuid = createUser("Colsson");
            api.setUserPermission(uuid, "anvil.fly", true, "admin", null);

            run("user", uuid.toString(), "permission", "remove", "anvil.fly");
            assertTrue(responses.stream().anyMatch(s -> s.contains("eliminado")));
            assertFalse(api.hasPermission(uuid, "anvil.fly"));
        }

        @Test
        void userPermissionClearWorks() {
            UUID uuid = createUser("Colsson");
            api.setUserPermission(uuid, "a", true, "admin", null);
            api.setUserPermission(uuid, "b", false, "admin", null);

            run("user", uuid.toString(), "permission", "clear");
            assertTrue(responses.stream().anyMatch(s -> s.contains("eliminados")));
            assertTrue(api.getUser(uuid).get().directPermissions().isEmpty());
        }

        @Test
        void userWithReasonWorks() {
            UUID uuid = createUser("Colsson");
            createGroupDb("VIP", 10);

            run("user", uuid.toString(), "group", "add", "VIP", "promoted");
            assertTrue(responses.stream().anyMatch(s -> s.contains("añadido")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Group commands
    // ══════════════════════════════════════════════════════════

    @Nested
    class GroupCommands {

        @Test
        void groupListShowsGroups() {
            createGroupDb("VIP", 10);
            createGroupDb("MOD", 20);

            run("group", "list");
            assertTrue(responses.stream().anyMatch(s -> s.contains("VIP")));
            assertTrue(responses.stream().anyMatch(s -> s.contains("MOD")));
        }

        @Test
        void groupCreateWorks() {
            run("group", "create", "VIP");
            assertTrue(responses.stream().anyMatch(s -> s.contains("creado")));
            assertTrue(api.getGroup("VIP").isPresent());
        }

        @Test
        void groupInfoWorks() {
            createGroupDb("VIP", 10);

            run("group", "VIP", "info");
            assertTrue(responses.stream().anyMatch(s -> s.contains("VIP")));
        }

        @Test
        void groupDeleteWorks() {
            createGroupDb("VIP", 10);

            run("group", "VIP", "delete");
            assertTrue(responses.stream().anyMatch(s -> s.contains("eliminado")));
            assertTrue(api.getGroup("VIP").isEmpty());
        }

        @Test
        void groupEditPriorityWorks() {
            createGroupDb("VIP", 10);

            run("group", "VIP", "edit", "priority", "99");
            assertTrue(responses.stream().anyMatch(s -> s.contains("99")));
            assertEquals(99, api.getGroup("VIP").get().priority());
        }

        @Test
        void groupEditNameWorks() {
            createGroupDb("VIP", 10);

            run("group", "VIP", "edit", "name", "VVIP");
            assertTrue(responses.stream().anyMatch(s -> s.contains("VVIP")));
            assertTrue(api.getGroup("VVIP").isPresent());
        }

        @Test
        void groupEditDescriptionWorks() {
            createGroupDb("VIP", 10);

            run("group", "VIP", "edit", "description", "Very Important");
            assertTrue(responses.stream().anyMatch(s -> s.contains("actualizada")));
        }

        @Test
        void groupParentSetWorks() {
            createGroupDb("MVP", 10);
            createGroupDb("MVP+", 20);

            run("group", "MVP+", "parent", "set", "MVP");
            assertTrue(responses.stream().anyMatch(s -> s.contains("hereda")));
            assertEquals(1, api.getParents("MVP+").size());
        }

        @Test
        void groupParentRemoveWorks() {
            createGroupDb("MVP", 10);
            createGroupDb("MVP+", 20);
            api.setParent("MVP+", "MVP", "admin", null);

            run("group", "MVP+", "parent", "remove", "MVP");
            assertTrue(responses.stream().anyMatch(s -> s.contains("ya no hereda")));
            assertTrue(api.getParents("MVP+").isEmpty());
        }

        @Test
        void groupParentsShowsList() {
            createGroupDb("MVP", 10);
            createGroupDb("MVP+", 20);
            api.setParent("MVP+", "MVP", "admin", null);

            run("group", "MVP+", "parents");
            assertTrue(responses.stream().anyMatch(s -> s.contains("MVP")));
        }

        @Test
        void groupPermissionSetWorks() {
            createGroupDb("VIP", 10);

            run("group", "VIP", "permission", "set", "anvil.fly", "true");
            assertTrue(responses.stream().anyMatch(s -> s.contains("Permiso")));
            assertEquals(PermissionState.TRUE, api.getGroup("VIP").get().getPermissionState("anvil.fly"));
        }

        @Test
        void groupPermissionRemoveWorks() {
            createGroupDb("VIP", 10);
            api.setGroupPermission("VIP", "anvil.fly", true, "admin", null);

            run("group", "VIP", "permission", "remove", "anvil.fly");
            assertEquals(PermissionState.UNDEFINED, api.getGroup("VIP").get().getPermissionState("anvil.fly"));
        }

        @Test
        void groupPermissionClearWorks() {
            createGroupDb("VIP", 10);
            api.setGroupPermission("VIP", "a", true, "admin", null);
            api.setGroupPermission("VIP", "b", false, "admin", null);

            run("group", "VIP", "permission", "clear");
            assertTrue(api.getGroup("VIP").get().permissions().isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Diagnostics
    // ══════════════════════════════════════════════════════════

    @Nested
    class Diagnostics {

        @Test
        void checkShowsResult() {
            UUID uuid = createUser("Colsson");
            createGroupDb("VIP", 10);
            api.addUserToGroup(uuid, "VIP", "admin", null);
            api.setGroupPermission("VIP", "anvil.fly", true, "admin", null);

            run("check", uuid.toString(), "anvil.fly");
            assertTrue(responses.stream().anyMatch(s -> s.contains("TRUE")));
        }

        @Test
        void groupsTreeShowsTree() {
            createGroupDb("MVP", 10);
            createGroupDb("MVP+", 20);
            api.setParent("MVP+", "MVP", "admin", null);

            run("groups", "tree");
            assertTrue(responses.stream().anyMatch(s -> s.contains("MVP")));
        }

        @Test
        void debugShowsInfo() {
            run("debug");
            assertTrue(responses.stream().anyMatch(s -> s.contains("Debug")));
        }

        @Test
        void debugUserShowsInfo() {
            UUID uuid = createUser("Colsson");
            run("debug", "user", uuid.toString());
            assertTrue(responses.stream().anyMatch(s -> s.contains("Colsson")));
        }

        @Test
        void debugPermissionShowsInfo() {
            UUID uuid = createUser("Colsson");
            run("debug", "permission", uuid.toString(), "anvil.fly");
            assertTrue(responses.stream().anyMatch(s -> s.contains("anvil.fly")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // History
    // ══════════════════════════════════════════════════════════

    @Nested
    class History {

        @Test
        void historyShowsRecent() {
            createGroupDb("VIP", 10);
            api.createGroup("MOD", 20, "", "admin", null);

            run("history");
            assertTrue(responses.stream().anyMatch(s -> s.contains("Historial")));
        }

        @Test
        void historyByGroupShowsEntries() {
            api.createGroup("VIP", 10, "", "admin", "test");

            run("history", "group", "VIP");
            assertTrue(responses.stream().anyMatch(s -> s.contains("VIP")));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Error handling
    // ══════════════════════════════════════════════════════════

    @Nested
    class Errors {

        @Test
        void unknownSubcommandShowsUsage() {
            run("nonexistent");
            assertTrue(responses.stream().anyMatch(s -> s.contains("Uso")));
        }

        @Test
        void missingArgsShowsError() {
            run("user");
            assertTrue(responses.stream().anyMatch(s -> s.contains("Uso")));
        }

        @Test
        void invalidBooleanShowsError() {
            UUID uuid = createUser("Colsson");
            run("user", uuid.toString(), "permission", "set", "anvil.fly", "maybe");
            assertTrue(responses.stream().anyMatch(s -> s.contains("inválido")));
        }

        @Test
        void invalidPriorityShowsError() {
            createGroupDb("VIP", 10);
            run("group", "VIP", "edit", "priority", "abc");
            assertTrue(responses.stream().anyMatch(s -> s.contains("inválida")));
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private void createGroupDb(String name, int priority) {
        try {
            new GroupRepository(db).create(name, priority, "");
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
