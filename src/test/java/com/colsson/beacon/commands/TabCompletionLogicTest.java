package com.colsson.beacon.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del motor de tab completion (SPEC §11).
 *
 * <p>Cubre todas las 40+ rutas documentadas.
 * Usa datos mock (no depende de Paper ni DB).
 */
class TabCompletionLogicTest {

    private static final List<String> PLAYERS = List.of("Colsson", "Steve", "Alex");
    private static final List<String> GROUPS = List.of("admin", "moderador", "vip", "default");

    private TabCompletionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TabCompletionEngine(() -> PLAYERS, () -> GROUPS);
    }

    // ══════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════

    private List<String> complete(String... args) {
        return engine.complete(args);
    }

    private void assertContains(List<String> result, String expected) {
        assertTrue(result.contains(expected),
            "Expected '" + expected + "' in " + result);
    }

    private void assertNotContains(List<String> result, String expected) {
        assertFalse(result.contains(expected),
            "Unexpected '" + expected + "' in " + result);
    }

    // ══════════════════════════════════════════════════════
    // §11.1 — Generales
    // ══════════════════════════════════════════════════════

    @Nested
    class GeneralCommands {

        @Test
        void empty_completesAllTopLevel() {
            var c = complete("");
            assertContains(c, "help");
            assertContains(c, "info");
            assertContains(c, "reload");
            assertContains(c, "user");
            assertContains(c, "group");
            assertContains(c, "permission");
            assertContains(c, "check");
            assertContains(c, "groups");
            assertContains(c, "debug");
            assertContains(c, "history");
        }

        @Test
        void partial_completesMatchingTopLevel() {
            var c = complete("u");
            assertContains(c, "user");
            assertNotContains(c, "help");
        }

        @Test
        void partialGroups_completesBothGroupAndGroups() {
            var c = complete("gro");
            assertContains(c, "group");
            assertContains(c, "groups");
        }

        @Test
        void unknownArg_returnsEmpty() {
            var c = complete("xyz");
            assertTrue(c.isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.2 — Usuario — Consultas
    // ══════════════════════════════════════════════════════

    @Nested
    class UserQueries {

        @Test
        void user_completesOnlinePlayers() {
            var c = complete("user", "");
            assertContains(c, "Colsson");
            assertContains(c, "Steve");
            assertContains(c, "Alex");
        }

        @Test
        void userPartial_completesMatchingPlayers() {
            var c = complete("user", "Co");
            assertContains(c, "Colsson");
            assertNotContains(c, "Steve");
        }

        @Test
        void userPlayer_completesQueryAndModifySubcommands() {
            var c = complete("user", "Colsson", "");
            assertContains(c, "info");
            assertContains(c, "groups");
            assertContains(c, "permissions");
            assertContains(c, "group");
            assertContains(c, "permission");
        }

        @Test
        void userPlayerPartial_completesMatching() {
            var c = complete("user", "Colsson", "i");
            assertContains(c, "info");
            assertNotContains(c, "groups");
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.3 — Usuario — Modificaciones
    // ══════════════════════════════════════════════════════

    @Nested
    class UserModifications {

        @Test
        void userGroup_completesAddRemove() {
            var c = complete("user", "Colsson", "group", "");
            assertContains(c, "add");
            assertContains(c, "remove");
        }

        @Test
        void userGroupAdd_completesGroupNames() {
            var c = complete("user", "Colsson", "group", "add", "");
            assertContains(c, "admin");
            assertContains(c, "vip");
            assertContains(c, "default");
        }

        @Test
        void userGroupAddPartial_completesMatchingGroups() {
            var c = complete("user", "Colsson", "group", "add", "v");
            assertContains(c, "vip");
            assertNotContains(c, "admin");
        }

        @Test
        void userGroupAddGroup_completesRazon() {
            var c = complete("user", "Colsson", "group", "add", "vip", "");
            assertContains(c, "<razón>");
        }

        @Test
        void userGroupRemove_completesGroupNames() {
            var c = complete("user", "Colsson", "group", "remove", "");
            assertContains(c, "admin");
            assertContains(c, "vip");
        }

        @Test
        void userGroupRemoveGroup_completesRazon() {
            var c = complete("user", "Colsson", "group", "remove", "vip", "");
            assertContains(c, "<razón>");
        }

        @Test
        void userPermission_completesSetRemoveClear() {
            var c = complete("user", "Colsson", "permission", "");
            assertContains(c, "set");
            assertContains(c, "remove");
            assertContains(c, "clear");
        }

        @Test
        void userPermissionSet_completesPermiso() {
            var c = complete("user", "Colsson", "permission", "set", "");
            assertContains(c, "<permiso>");
        }

        @Test
        void userPermissionSetPerm_completesTrueFalse() {
            var c = complete("user", "Colsson", "permission", "set", "test.permission", "");
            assertContains(c, "true");
            assertContains(c, "false");
        }

        @Test
        void userPermissionSetPermValue_completesRazon() {
            var c = complete("user", "Colsson", "permission", "set", "test.permission", "true", "");
            assertContains(c, "<razón>");
        }

        @Test
        void userPermissionRemove_completesPermiso() {
            var c = complete("user", "Colsson", "permission", "remove", "");
            assertContains(c, "<permiso>");
        }

        @Test
        void userPermissionRemovePerm_completesRazon() {
            var c = complete("user", "Colsson", "permission", "remove", "test.permission", "");
            assertContains(c, "<razón>");
        }

        @Test
        void userPermissionClear_completesRazon() {
            var c = complete("user", "Colsson", "permission", "clear", "");
            assertContains(c, "<razón>");
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.4 — Grupo — Gestión
    // ══════════════════════════════════════════════════════

    @Nested
    class GroupManagement {

        @Test
        void group_completesListCreate() {
            var c = complete("group", "");
            assertContains(c, "list");
            assertContains(c, "create");
        }

        @Test
        void groupCreate_completesNombre() {
            var c = complete("group", "create", "");
            assertContains(c, "<nombre>");
        }

        @Test
        void groupAdmin_completesSubcommands() {
            var c = complete("group", "admin", "");
            assertContains(c, "info");
            assertContains(c, "delete");
            assertContains(c, "edit");
            assertContains(c, "parent");
            assertContains(c, "permission");
            assertContains(c, "parents");
        }

        @Test
        void groupAdminPartial_completesMatching() {
            var c = complete("group", "admin", "e");
            assertContains(c, "edit");
            assertNotContains(c, "info");
        }

        @Test
        void groupAdminInfo_noMoreCompletions() {
            var c = complete("group", "admin", "info", "");
            assertTrue(c.isEmpty());
        }

        @Test
        void groupAdminDelete_completesRazon() {
            var c = complete("group", "admin", "delete", "");
            assertContains(c, "<razón>");
        }

        @Test
        void groupAdminParents_noMoreCompletions() {
            var c = complete("group", "admin", "parents", "");
            assertTrue(c.isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.5 — Grupo — Edición
    // ══════════════════════════════════════════════════════

    @Nested
    class GroupEdit {

        @Test
        void groupAdminEdit_completesFields() {
            var c = complete("group", "admin", "edit", "");
            assertContains(c, "name");
            assertContains(c, "priority");
            assertContains(c, "description");
        }

        @Test
        void groupAdminEditPartial_completesMatching() {
            var c = complete("group", "admin", "edit", "p");
            assertContains(c, "priority");
            assertNotContains(c, "name");
        }

        @Test
        void groupAdminEditName_completesNuevoNombre() {
            var c = complete("group", "admin", "edit", "name", "");
            assertContains(c, "<nuevoNombre>");
        }

        @Test
        void groupAdminEditPriority_completesNumero() {
            var c = complete("group", "admin", "edit", "priority", "");
            assertContains(c, "<numero>");
        }

        @Test
        void groupAdminEditDescription_completesTexto() {
            var c = complete("group", "admin", "edit", "description", "");
            assertContains(c, "<texto>");
        }

        @Test
        void groupAdminEditNameValue_completesRazon() {
            var c = complete("group", "admin", "edit", "name", "Nuevo", "");
            assertContains(c, "<razón>");
        }

        @Test
        void groupAdminEditPriorityValue_completesRazon() {
            var c = complete("group", "admin", "edit", "priority", "10", "");
            assertContains(c, "<razón>");
        }

        @Test
        void groupAdminEditDescriptionValue_completesRazon() {
            var c = complete("group", "admin", "edit", "description", "Texto", "");
            assertContains(c, "<razón>");
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.6 — Grupo — Herencia
    // ══════════════════════════════════════════════════════

    @Nested
    class GroupParent {

        @Test
        void groupAdminParent_completesSetRemove() {
            var c = complete("group", "admin", "parent", "");
            assertContains(c, "set");
            assertContains(c, "remove");
        }

        @Test
        void groupAdminParentSet_completesGroupNames() {
            var c = complete("group", "admin", "parent", "set", "");
            assertContains(c, "admin");
            assertContains(c, "vip");
        }

        @Test
        void groupAdminParentSetPartial_completesMatching() {
            var c = complete("group", "admin", "parent", "set", "d");
            assertContains(c, "default");
            assertNotContains(c, "admin");
        }

        @Test
        void groupAdminParentSetGroup_completesRazon() {
            var c = complete("group", "admin", "parent", "set", "vip", "");
            assertContains(c, "<razón>");
        }

        @Test
        void groupAdminParentRemove_completesGroupNames() {
            var c = complete("group", "admin", "parent", "remove", "");
            assertContains(c, "admin");
            assertContains(c, "vip");
        }

        @Test
        void groupAdminParentRemoveGroup_completesRazon() {
            var c = complete("group", "admin", "parent", "remove", "vip", "");
            assertContains(c, "<razón>");
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.7 — Grupo — Permisos
    // ══════════════════════════════════════════════════════

    @Nested
    class GroupPermission {

        @Test
        void groupAdminPermission_completesActions() {
            var c = complete("group", "admin", "permission", "");
            assertContains(c, "set");
            assertContains(c, "remove");
            assertContains(c, "clear");
        }

        @Test
        void groupAdminPermissionSet_completesPermiso() {
            var c = complete("group", "admin", "permission", "set", "");
            assertContains(c, "<permiso>");
        }

        @Test
        void groupAdminPermissionSetPerm_completesTrueFalse() {
            var c = complete("group", "admin", "permission", "set", "test.permission", "");
            assertContains(c, "true");
            assertContains(c, "false");
        }

        @Test
        void groupAdminPermissionSetPermValue_completesRazon() {
            var c = complete("group", "admin", "permission", "set", "test.permission", "true", "");
            assertContains(c, "<razón>");
        }

        @Test
        void groupAdminPermissionRemove_completesPermiso() {
            var c = complete("group", "admin", "permission", "remove", "");
            assertContains(c, "<permiso>");
        }

        @Test
        void groupAdminPermissionRemovePerm_completesRazon() {
            var c = complete("group", "admin", "permission", "remove", "test.permission", "");
            assertContains(c, "<razón>");
        }

        @Test
        void groupAdminPermissionClear_completesRazon() {
            var c = complete("group", "admin", "permission", "clear", "");
            assertContains(c, "<razón>");
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.8 — Permisos globales
    // ══════════════════════════════════════════════════════

    @Nested
    class GlobalPermissions {

        @Test
        void permission_completesListInfoSearch() {
            var c = complete("permission", "");
            assertContains(c, "list");
            assertContains(c, "info");
            assertContains(c, "search");
        }

        @Test
        void permissionInfo_completesPermiso() {
            var c = complete("permission", "info", "");
            assertContains(c, "<permiso>");
        }

        @Test
        void permissionSearch_completesTexto() {
            var c = complete("permission", "search", "");
            assertContains(c, "<texto>");
        }

        @Test
        void permissionList_noMoreCompletions() {
            var c = complete("permission", "list", "");
            assertTrue(c.isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.9 — Diagnóstico
    // ══════════════════════════════════════════════════════

    @Nested
    class Diagnostics {

        @Test
        void check_completesPlayers() {
            var c = complete("check", "");
            assertContains(c, "Colsson");
            assertContains(c, "Steve");
        }

        @Test
        void checkPlayer_completesPermiso() {
            var c = complete("check", "Colsson", "");
            assertContains(c, "<permiso>");
        }

        @Test
        void groups_completesTree() {
            var c = complete("groups", "");
            assertContains(c, "tree");
        }

        @Test
        void groupsTree_noMoreCompletions() {
            var c = complete("groups", "tree", "");
            assertTrue(c.isEmpty());
        }

        @Test
        void debug_completesUserPermission() {
            var c = complete("debug", "");
            assertContains(c, "user");
            assertContains(c, "permission");
        }

        @Test
        void debugUser_completesPlayers() {
            var c = complete("debug", "user", "");
            assertContains(c, "Colsson");
        }

        @Test
        void debugPermission_completesPlayers() {
            var c = complete("debug", "permission", "");
            assertContains(c, "Colsson");
        }

        @Test
        void debugPermissionPlayer_completesPermiso() {
            var c = complete("debug", "permission", "Colsson", "");
            assertContains(c, "<permiso>");
        }
    }

    // ══════════════════════════════════════════════════════
    // §11.10 — Historial
    // ══════════════════════════════════════════════════════

    @Nested
    class History {

        @Test
        void history_completesTypes() {
            var c = complete("history", "");
            assertContains(c, "user");
            assertContains(c, "group");
            assertContains(c, "permission");
        }

        @Test
        void historyUser_completesPlayers() {
            var c = complete("history", "user", "");
            assertContains(c, "Colsson");
        }

        @Test
        void historyGroup_completesGroupNames() {
            var c = complete("history", "group", "");
            assertContains(c, "admin");
            assertContains(c, "vip");
        }

        @Test
        void historyPermission_completesPermiso() {
            var c = complete("history", "permission", "");
            assertContains(c, "<permiso>");
        }
    }

    // ══════════════════════════════════════════════════════
    // Filtros de prefijo
    // ══════════════════════════════════════════════════════

    @Nested
    class Filtering {

        @Test
        void userGroup_completesOnlyMatching() {
            var c = complete("user", "Colsson", "group", "a");
            assertContains(c, "add");
            assertNotContains(c, "remove");
        }

        @Test
        void groupAdmin_completesOnlyMatching() {
            var c = complete("group", "admin", "e");
            assertContains(c, "edit");
            assertNotContains(c, "info");
        }

        @Test
        void debug_completesOnlyMatching() {
            var c = complete("debug", "u");
            assertContains(c, "user");
            assertNotContains(c, "permission");
        }

        @Test
        void history_completesOnlyMatching() {
            var c = complete("history", "p");
            assertContains(c, "permission");
            assertNotContains(c, "user");
        }
    }
}
