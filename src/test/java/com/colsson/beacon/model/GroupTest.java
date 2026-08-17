package com.colsson.beacon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroupTest {

    private Group defaultGroup;
    private Group vip;
    private Group mvp;
    private Group mod;
    private Group admin;

    @BeforeEach
    void setUp() {
        defaultGroup = new Group(1, "default", 0);
        vip = new Group(2, "VIP", 10);
        mvp = new Group(3, "MVP", 20);
        mod = new Group(4, "MOD", 50);
        admin = new Group(5, "ADMIN", 100);
    }

    @Test
    void createWithAllFields() {
        Group g = new Group(1, "VIP", 10, "Very Important Player");
        assertEquals(1, g.id());
        assertEquals("VIP", g.name());
        assertEquals(10, g.priority());
        assertEquals("Very Important Player", g.description());
    }

    @Test
    void createWithDefaultDescription() {
        Group g = new Group(1, "VIP", 10);
        assertEquals("", g.description());
    }

    @Test
    void rejectNullName() {
        assertThrows(IllegalArgumentException.class,
            () -> new Group(1, null, 0));
    }

    @Test
    void rejectBlankName() {
        assertThrows(IllegalArgumentException.class,
            () -> new Group(1, "  ", 0));
    }

    @Test
    void renameGroup() {
        vip.setName("VIP-ELITE");
        assertEquals("VIP-ELITE", vip.name());
    }

    @Test
    void renamePreservesIdentity() {
        long originalId = vip.id();
        vip.setName("VIP-ELITE");
        assertEquals(originalId, vip.id());
    }

    @Test
    void changePriority() {
        vip.setPriority(25);
        assertEquals(25, vip.priority());
    }

    @Test
    void changeDescription() {
        vip.setDescription("New description");
        assertEquals("New description", vip.description());
    }

    // ── Permisos ────────────────────────────────────────────

    @Test
    void setPermissionTrue() {
        vip.setPermission("anvil.fly", true);
        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly"));
    }

    @Test
    void setPermissionFalse() {
        vip.setPermission("anvil.fly", false);
        assertEquals(PermissionState.FALSE, vip.getPermissionState("anvil.fly"));
    }

    @Test
    void permissionUndefined() {
        assertEquals(PermissionState.UNDEFINED, vip.getPermissionState("anvil.fly"));
    }

    @Test
    void removePermission() {
        vip.setPermission("anvil.fly", true);
        assertTrue(vip.removePermission("anvil.fly"));
        assertEquals(PermissionState.UNDEFINED, vip.getPermissionState("anvil.fly"));
    }

    @Test
    void removePermissionReturnsFalseWhenNotSet() {
        assertFalse(vip.removePermission("anvil.fly"));
    }

    @Test
    void clearPermissions() {
        vip.setPermission("anvil.fly", true);
        vip.setPermission("anvil.kick", false);

        vip.clearPermissions();

        assertTrue(vip.permissions().isEmpty());
    }

    @Test
    void multiplePermissions() {
        vip.setPermission("anvil.fly", true);
        vip.setPermission("anvil.kick", true);
        vip.setPermission("anvil.home", true);

        assertEquals(3, vip.permissions().size());
    }

    @Test
    void rejectNullPermission() {
        assertThrows(IllegalArgumentException.class,
            () -> vip.setPermission(null, true));
    }

    @Test
    void overwritePermission() {
        vip.setPermission("anvil.fly", true);
        vip.setPermission("anvil.fly", false);
        assertEquals(PermissionState.FALSE, vip.getPermissionState("anvil.fly"));
        assertEquals(1, vip.permissions().size());
    }

    // ── Herencia ────────────────────────────────────────────

    @Test
    void addParent() {
        mvp.addParent(vip);

        assertTrue(mvp.parents().contains(vip));
        assertTrue(vip.children().contains(mvp));
    }

    @Test
    void addParentIsIdempotent() {
        mvp.addParent(vip);
        mvp.addParent(vip);
        assertEquals(1, mvp.parents().size());
    }

    @Test
    void removeParent() {
        mvp.addParent(vip);
        assertTrue(mvp.removeParent(vip));

        assertFalse(mvp.parents().contains(vip));
        assertFalse(vip.children().contains(mvp));
    }

    @Test
    void removeParentReturnsFalseWhenNotParent() {
        assertFalse(mvp.removeParent(vip));
    }

    @Test
    void rejectSelfInheritance() {
        assertThrows(CycleDetectedException.class,
            () -> mvp.addParent(mvp));
    }

    @Test
    void rejectDirectCycle() {
        mvp.addParent(vip);
        assertThrows(CycleDetectedException.class,
            () -> vip.addParent(mvp));
    }

    @Test
    void rejectIndirectCycle() {
        defaultGroup.addParent(vip);
        vip.addParent(mvp);
        assertThrows(CycleDetectedException.class,
            () -> mvp.addParent(defaultGroup));
    }

    @Test
    void rejectNullParent() {
        assertThrows(IllegalArgumentException.class,
            () -> mvp.addParent(null));
    }

    // ── Ancestros ───────────────────────────────────────────

    @Test
    void ancestorsSingleLevel() {
        mvp.addParent(vip);

        Set<Group> ancestors = mvp.ancestors();
        assertTrue(ancestors.contains(vip));
        assertEquals(1, ancestors.size());
    }

    @Test
    void ancestorsMultiLevel() {
        // MVP → VIP → default
        mvp.addParent(vip);
        vip.addParent(defaultGroup);

        Set<Group> ancestors = mvp.ancestors();
        assertTrue(ancestors.contains(vip));
        assertTrue(ancestors.contains(defaultGroup));
        assertEquals(2, ancestors.size());
    }

    @Test
    void ancestorsDiamond() {
        //      root
        //     /    \
        //    A      B
        //     \    /
        //      leaf
        Group root = new Group(10, "root", 0);
        Group a = new Group(11, "A", 10);
        Group b = new Group(12, "B", 10);
        Group leaf = new Group(13, "leaf", 30);

        a.addParent(root);
        b.addParent(root);
        leaf.addParent(a);
        leaf.addParent(b);

        Set<Group> ancestors = leaf.ancestors();
        assertTrue(ancestors.contains(a));
        assertTrue(ancestors.contains(b));
        assertTrue(ancestors.contains(root));
        assertEquals(3, ancestors.size());
    }

    @Test
    void ancestorsEmpty() {
        assertTrue(mvp.ancestors().isEmpty());
    }

    // ── Miembros ────────────────────────────────────────────

    @Test
    void membersTrackUser() {
        User user = new User(UUID.randomUUID(), "Colsson");
        user.addGroup(vip);

        assertTrue(vip.members().contains(user));
    }

    @Test
    void removeGroupRemovesMember() {
        User user = new User(UUID.randomUUID(), "Colsson");
        user.addGroup(vip);
        user.removeGroup(vip);

        assertFalse(vip.members().contains(user));
    }

    // ── Identidad ───────────────────────────────────────────

    @Test
    void equalityById() {
        Group a = new Group(1, "VIP", 10);
        Group b = new Group(1, "DifferentName", 20);
        assertEquals(a, b);
    }

    @Test
    void inequalityByDifferentId() {
        Group a = new Group(1, "VIP", 10);
        Group b = new Group(2, "VIP", 10);
        assertNotEquals(a, b);
    }

    // ── World-specific permissions ──────────────────────────

    @Test
    void worldSpecificPermissionTrue() {
        vip.setPermission("anvil.fly", true, "lobby");
        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly", "lobby"));
    }

    @Test
    void worldSpecificPermissionFalse() {
        vip.setPermission("anvil.fly", false, "skyblock");
        assertEquals(PermissionState.FALSE, vip.getPermissionState("anvil.fly", "skyblock"));
    }

    @Test
    void worldSpecificUndefinedDoesNotBlockGlobal() {
        vip.setPermission("anvil.fly", true);  // global

        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly", "skyblock"));
    }

    @Test
    void worldSpecificOverridesGlobal() {
        vip.setPermission("anvil.fly", true);              // global
        vip.setPermission("anvil.fly", false, "skyblock"); // world-specific

        assertEquals(PermissionState.FALSE, vip.getPermissionState("anvil.fly", "skyblock"));
        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly", "lobby"));
    }

    @Test
    void worldSpecificRemoveOnlyAffectsWorld() {
        vip.setPermission("anvil.fly", false, "skyblock");
        vip.setPermission("anvil.fly", true);  // global

        assertTrue(vip.removePermission("anvil.fly", "skyblock"));

        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly", "skyblock"));
        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly"));
    }

    @Test
    void removePermissionFromAllWorlds() {
        vip.setPermission("anvil.fly", false, "skyblock");
        vip.setPermission("anvil.fly", true);  // global

        assertTrue(vip.removePermission("anvil.fly"));

        assertEquals(PermissionState.UNDEFINED, vip.getPermissionState("anvil.fly", "skyblock"));
        assertEquals(PermissionState.UNDEFINED, vip.getPermissionState("anvil.fly"));
    }

    @Test
    void clearPermissionsByWorld() {
        vip.setPermission("anvil.fly", false, "skyblock");
        vip.setPermission("anvil.kick", true, "skyblock");
        vip.setPermission("anvil.fly", true);  // global

        vip.clearPermissions("skyblock");

        // skyblock cleared → cae al global
        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly", "skyblock"));
        assertEquals(PermissionState.UNDEFINED, vip.getPermissionState("anvil.kick", "skyblock"));
        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly"));
    }

    @Test
    void multipleWorldsIndependent() {
        vip.setPermission("anvil.fly", true, "lobby");
        vip.setPermission("anvil.fly", false, "skyblock");
        vip.setPermission("anvil.fly", true, "survival");

        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly", "lobby"));
        assertEquals(PermissionState.FALSE, vip.getPermissionState("anvil.fly", "skyblock"));
        assertEquals(PermissionState.TRUE, vip.getPermissionState("anvil.fly", "survival"));
    }

    @Test
    void worldsReturnsAllWorldsWithPermissions() {
        vip.setPermission("anvil.fly", true, "lobby");
        vip.setPermission("anvil.fly", false, "skyblock");
        vip.setPermission("anvil.kick", true);  // global

        Set<String> worlds = vip.worlds();
        assertEquals(3, worlds.size());
        assertTrue(worlds.contains(null));
        assertTrue(worlds.contains("lobby"));
        assertTrue(worlds.contains("skyblock"));
    }

    @Test
    void permissionsByWorld() {
        vip.setPermission("anvil.fly", true, "lobby");
        vip.setPermission("anvil.kick", false, "lobby");
        vip.setPermission("anvil.fly", false, "skyblock");

        var lobbyPerms = vip.permissions("lobby");
        assertEquals(2, lobbyPerms.size());

        var skyblockPerms = vip.permissions("skyblock");
        assertEquals(1, skyblockPerms.size());

        var survivalPerms = vip.permissions("survival");
        assertTrue(survivalPerms.isEmpty());
    }

    @Test
    void permissionCountIncludesAllWorlds() {
        vip.setPermission("anvil.fly", true, "lobby");
        vip.setPermission("anvil.fly", false, "skyblock");
        vip.setPermission("anvil.kick", true);  // global

        assertEquals(3, vip.permissionCount());
    }

    @Test
    void hasPermissionWithWorld() {
        vip.setPermission("anvil.fly", true, "lobby");

        assertTrue(vip.hasPermission("anvil.fly", "lobby"));
        assertFalse(vip.hasPermission("anvil.fly", "skyblock"));
    }
}
