package com.colsson.beacon.resolver;

import com.colsson.beacon.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PermissionResolverTest {

    private PermissionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PermissionResolver();
    }

    // ── Utilidades ──────────────────────────────────────────

    private User user(String name) {
        return new User(UUID.randomUUID(), name);
    }

    // ══════════════════════════════════════════════════════════
    // §6.1 — Permisos directos del usuario
    // ══════════════════════════════════════════════════════════

    @Nested
    class DirectPermissions {

        @Test
        void directTrue() {
            User u = user("A");
            u.setDirectPermission("anvil.fly", true);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("direct", result.source());
        }

        @Test
        void directFalse() {
            User u = user("A");
            u.setDirectPermission("anvil.fly", false);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("direct", result.source());
        }

        @Test
        void directOverridesGroup() {
            // MVP++ → anvil.fly = TRUE
            // Colsson → anvil.fly = FALSE (excepción directa)
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.setDirectPermission("anvil.fly", false);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("direct", result.source());
        }

        @Test
        void directTrueOverridesGroupFalse() {
            // MOD → anvil.fly = FALSE
            // Colsson → anvil.fly = TRUE (excepción directa)
            Group mod = new Group(1, "MOD", 50);
            mod.setPermission("anvil.fly", false);

            User u = user("Colsson");
            u.addGroup(mod);
            u.setDirectPermission("anvil.fly", true);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("direct", result.source());
        }
    }

    // ══════════════════════════════════════════════════════════
    // §6.2 — Resolución con un solo grupo
    // ══════════════════════════════════════════════════════════

    @Nested
    class SingleGroup {

        @Test
        void groupTrue() {
            Group g = new Group(1, "VIP", 10);
            g.setPermission("anvil.fly", true);

            User u = user("A");
            u.addGroup(g);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("VIP", result.source());
        }

        @Test
        void groupFalse() {
            Group g = new Group(1, "VIP", 10);
            g.setPermission("anvil.fly", false);

            User u = user("A");
            u.addGroup(g);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("VIP", result.source());
        }

        @Test
        void groupUndefined() {
            Group g = new Group(1, "VIP", 10);

            User u = user("A");
            u.addGroup(g);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.UNDEFINED, result.state());
        }

        @Test
        void noGroupsNoDirect() {
            User u = user("A");

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.UNDEFINED, result.state());
        }
    }

    // ══════════════════════════════════════════════════════════
    // §6.3 — Conflicto entre grupos (prioridad)
    // ══════════════════════════════════════════════════════════

    @Nested
    class PriorityConflict {

        @Test
        void higherPriorityWins() {
            // MVP++ (30) → TRUE
            // MOD (50) → FALSE
            // MOD tiene mayor prioridad → FALSE
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            Group mod = new Group(2, "MOD", 50);
            mod.setPermission("anvil.fly", false);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.addGroup(mod);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("MOD", result.source());
        }

        @Test
        void lowerPriorityLoses() {
            // MOD (50) → FALSE
            // ADMIN (100) → TRUE
            // ADMIN tiene mayor prioridad → TRUE
            Group mod = new Group(1, "MOD", 50);
            mod.setPermission("anvil.fly", false);

            Group admin = new Group(2, "ADMIN", 100);
            admin.setPermission("anvil.fly", true);

            User u = user("Colsson");
            u.addGroup(mod);
            u.addGroup(admin);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("ADMIN", result.source());
        }

        @Test
        void trueAndFalseSamePriorityFirstWins() {
            // Si dos grupos tienen la misma prioridad, el primero en la lista gana
            Group a = new Group(1, "A", 10);
            a.setPermission("anvil.fly", true);

            Group b = new Group(2, "B", 10);
            b.setPermission("anvil.fly", false);

            User u = user("User");
            u.addGroup(a);
            u.addGroup(b);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            // Con misma prioridad, el primero insertado (A) gana
            assertEquals(PermissionState.TRUE, result.state());
        }
    }

    // ══════════════════════════════════════════════════════════
    // §6.4 — UNDEFINED no compite
    // ══════════════════════════════════════════════════════════

    @Nested
    class UndefinedBehavior {

        @Test
        void trueAndUndefinedReturnsTrue() {
            // MVP++ → TRUE
            // MOD → UNDEFINED (no tiene el permiso)
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            Group mod = new Group(2, "MOD", 50);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.addGroup(mod);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("MVP++", result.source());
        }

        @Test
        void falseAndUndefinedReturnsFalse() {
            // MVP++ → FALSE
            // MOD → UNDEFINED
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", false);

            Group mod = new Group(2, "MOD", 50);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.addGroup(mod);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("MVP++", result.source());
        }

        @Test
        void allUndefinedReturnsUndefined() {
            Group a = new Group(1, "A", 10);
            Group b = new Group(2, "B", 20);

            User u = user("User");
            u.addGroup(a);
            u.addGroup(b);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.UNDEFINED, result.state());
        }

        @Test
        void higherPriorityUndefinedDoesNotOverride() {
            // VIP (10) → TRUE
            // MOD (50) → UNDEFINED
            // El UNDEFINED de MOD no bloquea el TRUE de VIP
            Group vip = new Group(1, "VIP", 10);
            vip.setPermission("anvil.fly", true);

            Group mod = new Group(2, "MOD", 50);

            User u = user("Colsson");
            u.addGroup(vip);
            u.addGroup(mod);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("VIP", result.source());
        }
    }

    // ══════════════════════════════════════════════════════════
    // §8 — Herencia
    // ══════════════════════════════════════════════════════════

    @Nested
    class Inheritance {

        @Test
        void singleLevelInheritance() {
            // MVP+ hereda de MVP
            // MVP → anvil.fly = TRUE
            Group mvp = new Group(1, "MVP", 10);
            mvp.setPermission("anvil.fly", true);

            Group mvpPlus = new Group(2, "MVP+", 20);
            mvpPlus.addParent(mvp);

            User u = user("A");
            u.addGroup(mvpPlus);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("MVP", result.source());
        }

        @Test
        void multiLevelInheritance() {
            // MVP++ → MVP+ → MVP
            // MVP → anvil.fly = TRUE
            Group mvp = new Group(1, "MVP", 10);
            mvp.setPermission("anvil.fly", true);

            Group mvpPlus = new Group(2, "MVP+", 20);
            mvpPlus.addParent(mvp);

            Group mvpElite = new Group(3, "MVP++", 30);
            mvpElite.addParent(mvpPlus);

            User u = user("A");
            u.addGroup(mvpElite);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("MVP", result.source());
        }

        @Test
        void childOverridesParent() {
            // MVP+ → anvil.fly = FALSE
            // MVP → anvil.fly = TRUE
            // El hijo tiene la misma prioridad que el padre,
            // pero el hijo se evalúa primero (directamente del grupo)
            Group mvp = new Group(1, "MVP", 10);
            mvp.setPermission("anvil.fly", true);

            Group mvpPlus = new Group(2, "MVP+", 10);
            mvpPlus.setPermission("anvil.fly", false);
            mvpPlus.addParent(mvp);

            User u = user("A");
            u.addGroup(mvpPlus);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("MVP+", result.source());
        }

        @Test
        void ancestorWithHigherPriorityWins() {
            // STAFF I hereda de STAFF BASE
            // STAFF BASE (prioridad 100) → anvil.fly = TRUE
            // STAFF I (prioridad 50) → no tiene anvil.fly
            // STAFF BASE gana por mayor prioridad
            Group staffBase = new Group(1, "STAFF-BASE", 100);
            staffBase.setPermission("anvil.fly", true);

            Group staff1 = new Group(2, "STAFF-I", 50);
            staff1.addParent(staffBase);

            User u = user("Mod");
            u.addGroup(staff1);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("STAFF-BASE", result.source());
        }

        @Test
        void ancestorOverridesChildWhenHigherPriority() {
            // STAFF I (50) → anvil.fly = FALSE
            // STAFF BASE (100) → anvil.fly = TRUE
            // El ancestro tiene mayor prioridad → TRUE gana
            Group staffBase = new Group(1, "STAFF-BASE", 100);
            staffBase.setPermission("anvil.fly", true);

            Group staff1 = new Group(2, "STAFF-I", 50);
            staff1.setPermission("anvil.fly", false);
            staff1.addParent(staffBase);

            User u = user("Mod");
            u.addGroup(staff1);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("STAFF-BASE", result.source());
        }
    }

    // ══════════════════════════════════════════════════════════
    // §6.5 — Restauración por eliminación
    // ══════════════════════════════════════════════════════════

    @Nested
    class RestorationByRemoval {

        @Test
        void removingDirectExceptionRestoresGroupResolution() {
            // MVP++ → anvil.fly = TRUE
            // Colsson → anvil.fly = FALSE (excepción directa)
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.setDirectPermission("anvil.fly", false);

            // Verificar que la excepción aplica
            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.fly").state());

            // Eliminar la excepción directa
            u.removeDirectPermission("anvil.fly");

            // Verificar que se restaura la resolución natural
            PermissionResult result = resolver.resolve(u, "anvil.fly");
            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("MVP++", result.source());
        }

        @Test
        void removingDirectExceptionRestoresUndefined() {
            // Sin permiso en grupo, con excepción directa FALSE
            Group g = new Group(1, "VIP", 10);

            User u = user("A");
            u.addGroup(g);
            u.setDirectPermission("anvil.fly", false);

            // Excepción directa → FALSE
            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.fly").state());

            // Eliminar excepción → UNDEFINED (el grupo no tiene el permiso)
            u.removeDirectPermission("anvil.fly");

            PermissionResult result = resolver.resolve(u, "anvil.fly");
            assertEquals(PermissionState.UNDEFINED, result.state());
        }
    }

    // ══════════════════════════════════════════════════════════
    // permission set false vs permission remove
    // ══════════════════════════════════════════════════════════

    @Nested
    class SetFalseVsRemove {

        @Test
        void setFalseStoresFalse() {
            Group g = new Group(1, "VIP", 10);
            g.setPermission("anvil.fly", false);

            assertEquals(PermissionState.FALSE, g.getPermissionState("anvil.fly"));
        }

        @Test
        void removeStoresUndefined() {
            Group g = new Group(1, "VIP", 10);
            g.setPermission("anvil.fly", false);
            g.removePermission("anvil.fly");

            assertEquals(PermissionState.UNDEFINED, g.getPermissionState("anvil.fly"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Trazabilidad
    // ══════════════════════════════════════════════════════════

    @Nested
    class Traceability {

        @Test
        void traceShowsSource() {
            Group g = new Group(1, "VIP", 10);
            g.setPermission("anvil.fly", true);

            User u = user("A");
            u.addGroup(g);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertNotNull(result.trace());
            assertTrue(result.trace().contains("VIP"));
            assertTrue(result.trace().contains("TRUE"));
        }

        @Test
        void traceShowsConflictResolution() {
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            Group mod = new Group(2, "MOD", 50);
            mod.setPermission("anvil.fly", false);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.addGroup(mod);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertNotNull(result.trace());
            assertTrue(result.trace().contains("MOD"));
            assertTrue(result.trace().contains("MVP++"));
        }

        @Test
        void directSourceShowsDirect() {
            User u = user("A");
            u.setDirectPermission("anvil.fly", true);

            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals("direct", result.source());
            assertTrue(result.trace().contains("Direct"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // Edge cases
    // ══════════════════════════════════════════════════════════

    @Nested
    class EdgeCases {

        @Test
        void nullUserReturnsUndefined() {
            PermissionResult result = resolver.resolve(null, "anvil.fly");
            assertEquals(PermissionState.UNDEFINED, result.state());
        }

        @Test
        void nullPermissionReturnsUndefined() {
            User u = user("A");
            PermissionResult result = resolver.resolve(u, null);
            assertEquals(PermissionState.UNDEFINED, result.state());
        }

        @Test
        void blankPermissionReturnsUndefined() {
            User u = user("A");
            PermissionResult result = resolver.resolve(u, "  ");
            assertEquals(PermissionState.UNDEFINED, result.state());
        }

        @Test
        void differentPermissionsDoNotInterfere() {
            Group g = new Group(1, "VIP", 10);
            g.setPermission("anvil.fly", true);
            g.setPermission("anvil.kick", false);

            User u = user("A");
            u.addGroup(g);

            assertEquals(PermissionState.TRUE, resolver.resolve(u, "anvil.fly").state());
            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.kick").state());
            assertEquals(PermissionState.UNDEFINED, resolver.resolve(u, "anvil.home").state());
        }

        @Test
        void multipleGroupsMultiplePermissions() {
            // MVP++ → anvil.fly = TRUE, anvil.kick = TRUE
            // MOD → anvil.fly = FALSE, anvil.home = TRUE
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);
            mvp.setPermission("anvil.kick", true);

            Group mod = new Group(2, "MOD", 50);
            mod.setPermission("anvil.fly", false);
            mod.setPermission("anvil.home", true);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.addGroup(mod);

            // fly: MOD (50, FALSE) gana sobre MVP++ (30, TRUE)
            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.fly").state());
            // kick: solo MVP++ lo tiene → TRUE
            assertEquals(PermissionState.TRUE, resolver.resolve(u, "anvil.kick").state());
            // home: solo MOD lo tiene → TRUE
            assertEquals(PermissionState.TRUE, resolver.resolve(u, "anvil.home").state());
            // tpa: nadie lo tiene → UNDEFINED
            assertEquals(PermissionState.UNDEFINED, resolver.resolve(u, "anvil.tpa").state());
        }

        @Test
        void inheritanceWithConflictBetweenAncestorAndDirectGroup() {
            // MVP++ hereda de MVP
            // MVP → anvil.fly = TRUE (priority 10)
            // MOD → anvil.fly = FALSE (priority 50)
            // MVP++ → anvil.fly = TRUE (priority 30, directo del grupo)
            Group mvp = new Group(1, "MVP", 10);
            mvp.setPermission("anvil.fly", true);

            Group mvpPlus = new Group(2, "MVP++", 30);
            mvpPlus.addParent(mvp);
            // MVP++ no define anvil.fly directamente, hereda de MVP

            Group mod = new Group(3, "MOD", 50);
            mod.setPermission("anvil.fly", false);

            User u = user("Colsson");
            u.addGroup(mvpPlus);
            u.addGroup(mod);

            // Fuentes definidas:
            // - MVP++ → hereda de MVP → TRUE (priority 10, del ancestro)
            // - MOD → FALSE (priority 50)
            // MOD gana por mayor prioridad
            PermissionResult result = resolver.resolve(u, "anvil.fly");

            assertEquals(PermissionState.FALSE, result.state());
            assertEquals("MOD", result.source());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Escenarios del instrumento §61
    // ══════════════════════════════════════════════════════════

    @Nested
    class InstrumentScenarios {

        @Test
        void casoA_groupTrue() {
            Group g = new Group(1, "MVP++", 30);
            g.setPermission("anvil.fly", true);

            User u = user("A");
            u.addGroup(g);

            assertEquals(PermissionState.TRUE, resolver.resolve(u, "anvil.fly").state());
        }

        @Test
        void casoB_groupFalse() {
            Group g = new Group(1, "MVP++", 30);
            g.setPermission("anvil.fly", false);

            User u = user("A");
            u.addGroup(g);

            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.fly").state());
        }

        @Test
        void casoC_groupUndefined() {
            Group g = new Group(1, "MVP++", 30);

            User u = user("A");
            u.addGroup(g);

            assertEquals(PermissionState.UNDEFINED, resolver.resolve(u, "anvil.fly").state());
        }

        @Test
        void casoD_twoGroupsPriorityDetermines() {
            // MVP++ (30) → TRUE, MOD (50) → FALSE
            // MOD gana
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            Group mod = new Group(2, "MOD", 50);
            mod.setPermission("anvil.fly", false);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.addGroup(mod);

            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.fly").state());
            assertEquals("MOD", resolver.resolve(u, "anvil.fly").source());
        }

        @Test
        void casoE_directExceptionOverridesGroup() {
            // MVP++ → TRUE, Colsson direct → FALSE
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.setDirectPermission("anvil.fly", false);

            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.fly").state());
            assertEquals("direct", resolver.resolve(u, "anvil.fly").source());
        }

        @Test
        void casoF_removeExceptionRestoresNatural() {
            // MVP++ → TRUE, Colsson direct → FALSE
            // Remove direct → TRUE restored
            Group mvp = new Group(1, "MVP++", 30);
            mvp.setPermission("anvil.fly", true);

            User u = user("Colsson");
            u.addGroup(mvp);
            u.setDirectPermission("anvil.fly", false);

            // Before remove: FALSE
            assertEquals(PermissionState.FALSE, resolver.resolve(u, "anvil.fly").state());

            // Remove direct exception
            u.removeDirectPermission("anvil.fly");

            // After remove: TRUE restored from group
            PermissionResult result = resolver.resolve(u, "anvil.fly");
            assertEquals(PermissionState.TRUE, result.state());
            assertEquals("MVP++", result.source());
        }
    }

    // ══════════════════════════════════════════════════════════
    // World-specific permissions
    // ══════════════════════════════════════════════════════════

    @Nested
    class WorldSpecific {

        @Test
        void worldSpecificDirectOverridesGlobal() {
            User u = user("Colsson");
            u.setDirectPermission("anvil.fly", true);              // global
            u.setDirectPermission("anvil.fly", false, "skyblock"); // world-specific

            // skyblock: world-specific FALSE gana
            assertEquals(PermissionState.FALSE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
            // lobby: usa global TRUE
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "lobby").state());
        }

        @Test
        void worldSpecificGroupOverridesGlobalGroup() {
            User u = user("Colsson");
            Group defaultGroup = new Group(1, "default", 0);
            defaultGroup.setPermission("anvil.fly", true);              // global
            defaultGroup.setPermission("anvil.fly", false, "skyblock"); // world-specific
            u.addGroup(defaultGroup);

            assertEquals(PermissionState.FALSE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "lobby").state());
        }

        @Test
        void worldSpecificUndefinedFallsToGlobal() {
            User u = user("Colsson");
            Group defaultGroup = new Group(1, "default", 0);
            defaultGroup.setPermission("anvil.fly", true); // global only
            u.addGroup(defaultGroup);

            // skyblock no tiene world-specific → usa global
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
        }

        @Test
        void worldSpecificAncestorOverridesGlobal() {
            User u = user("Colsson");
            Group child = new Group(1, "child", 10);
            Group parent = new Group(2, "parent", 0);
            parent.setPermission("anvil.fly", true);              // global
            parent.setPermission("anvil.fly", false, "skyblock"); // world-specific
            child.addParent(parent);
            u.addGroup(child);

            assertEquals(PermissionState.FALSE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "lobby").state());
        }

        @Test
        void worldSpecificPriorityBetweenGroups() {
            User u = user("Colsson");
            Group low = new Group(1, "low", 10);
            Group high = new Group(2, "high", 50);
            low.setPermission("anvil.fly", true, "skyblock");
            high.setPermission("anvil.fly", false, "skyblock");
            u.addGroup(low);
            u.addGroup(high);

            // high (priority 50) gana sobre low (priority 10)
            assertEquals(PermissionState.FALSE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
        }

        @Test
        void worldSpecificVsGlobalPriority() {
            User u = user("Colsson");
            Group low = new Group(1, "low", 10);
            Group high = new Group(2, "high", 50);
            low.setPermission("anvil.fly", true, "skyblock"); // world-specific
            high.setPermission("anvil.fly", true);            // global
            u.addGroup(low);
            u.addGroup(high);

            // Both TRUE → TRUE wins regardless
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
        }

        @Test
        void nullWorldResolvesGlobal() {
            User u = user("Colsson");
            u.setDirectPermission("anvil.fly", true);

            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", null).state());
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly").state());
        }

        @Test
        void worldSpecificTraceIncludesWorld() {
            User u = user("Colsson");
            u.setDirectPermission("anvil.fly", false, "skyblock");

            PermissionResult result = resolver.resolve(u, "anvil.fly", "skyblock");
            assertTrue(result.trace().contains("skyblock"));
        }

        @Test
        void worldSpecificSourceIncludesWorld() {
            User u = user("Colsson");
            u.setDirectPermission("anvil.fly", false, "skyblock");

            PermissionResult result = resolver.resolve(u, "anvil.fly", "skyblock");
            assertTrue(result.source().contains("skyblock"));
        }

        @Test
        void multipleWorldsIndependent() {
            User u = user("Colsson");
            Group g = new Group(1, "default", 0);
            g.setPermission("anvil.fly", true, "lobby");
            g.setPermission("anvil.fly", false, "skyblock");
            g.setPermission("anvil.fly", true, "survival");
            u.addGroup(g);

            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "lobby").state());
            assertEquals(PermissionState.FALSE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "survival").state());
        }

        @Test
        void worldSpecificRemoveRestoresGlobal() {
            User u = user("Colsson");
            Group g = new Group(1, "default", 0);
            g.setPermission("anvil.fly", true);              // global
            g.setPermission("anvil.fly", false, "skyblock"); // world-specific
            u.addGroup(g);

            // Initially FALSE in skyblock
            assertEquals(PermissionState.FALSE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());

            // Remove world-specific
            g.removePermission("anvil.fly", "skyblock");

            // Now falls to global TRUE
            assertEquals(PermissionState.TRUE,
                resolver.resolve(u, "anvil.fly", "skyblock").state());
        }
    }
}
