package com.colsson.beacon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "Colsson");
    }

    @Test
    void createWithValidData() {
        assertEquals(UUID.class, user.uuid().getClass());
        assertEquals("Colsson", user.username());
        assertTrue(user.groups().isEmpty());
        assertTrue(user.directPermissions().isEmpty());
    }

    @Test
    void rejectNullUuid() {
        assertThrows(IllegalArgumentException.class,
            () -> new User(null, "Colsson"));
    }

    @Test
    void rejectNullUsername() {
        assertThrows(IllegalArgumentException.class,
            () -> new User(UUID.randomUUID(), null));
    }

    @Test
    void rejectBlankUsername() {
        assertThrows(IllegalArgumentException.class,
            () -> new User(UUID.randomUUID(), "  "));
    }

    @Test
    void updateUsername() {
        user.setUsername("NewName");
        assertEquals("NewName", user.username());
    }

    @Test
    void updateUsernameRejectsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> user.setUsername(null));
    }

    @Test
    void addGroup() {
        Group group = new Group(1, "VIP", 10);
        user.addGroup(group);

        assertTrue(user.hasGroup(group));
        assertEquals(1, user.groups().size());
        assertTrue(group.members().contains(user));
    }

    @Test
    void addMultipleGroups() {
        Group vip = new Group(1, "VIP", 10);
        Group mod = new Group(2, "MOD", 50);

        user.addGroup(vip);
        user.addGroup(mod);

        assertEquals(2, user.groups().size());
        assertTrue(user.hasGroup(vip));
        assertTrue(user.hasGroup(mod));
    }

    @Test
    void removeGroup() {
        Group group = new Group(1, "VIP", 10);
        user.addGroup(group);
        assertTrue(user.removeGroup(group));

        assertFalse(user.hasGroup(group));
        assertTrue(user.groups().isEmpty());
        assertFalse(group.members().contains(user));
    }

    @Test
    void removeGroupReturnsFalseWhenNotMember() {
        Group group = new Group(1, "VIP", 10);
        assertFalse(user.removeGroup(group));
    }

    @Test
    void addSameGroupTwiceIsIdempotent() {
        Group group = new Group(1, "VIP", 10);
        user.addGroup(group);
        user.addGroup(group);

        assertEquals(1, user.groups().size());
    }

    @Test
    void directPermissionTrue() {
        user.setDirectPermission("anvil.fly", true);
        assertEquals(PermissionState.TRUE, user.getDirectPermissionState("anvil.fly"));
    }

    @Test
    void directPermissionFalse() {
        user.setDirectPermission("anvil.fly", false);
        assertEquals(PermissionState.FALSE, user.getDirectPermissionState("anvil.fly"));
    }

    @Test
    void directPermissionUndefined() {
        assertEquals(PermissionState.UNDEFINED, user.getDirectPermissionState("anvil.fly"));
    }

    @Test
    void removeDirectPermission() {
        user.setDirectPermission("anvil.fly", false);
        assertTrue(user.removeDirectPermission("anvil.fly"));
        assertEquals(PermissionState.UNDEFINED, user.getDirectPermissionState("anvil.fly"));
    }

    @Test
    void removeDirectPermissionReturnsFalseWhenNotSet() {
        assertFalse(user.removeDirectPermission("anvil.fly"));
    }

    @Test
    void clearDirectPermissions() {
        user.setDirectPermission("anvil.fly", true);
        user.setDirectPermission("anvil.kick", false);

        user.clearDirectPermissions();

        assertTrue(user.directPermissions().isEmpty());
        assertEquals(PermissionState.UNDEFINED, user.getDirectPermissionState("anvil.fly"));
        assertEquals(PermissionState.UNDEFINED, user.getDirectPermissionState("anvil.kick"));
    }

    @Test
    void multipleDirectPermissions() {
        user.setDirectPermission("anvil.fly", true);
        user.setDirectPermission("anvil.kick", false);
        user.setDirectPermission("loom.chat.color", true);

        assertEquals(3, user.directPermissions().size());
        assertEquals(PermissionState.TRUE, user.getDirectPermissionState("anvil.fly"));
        assertEquals(PermissionState.FALSE, user.getDirectPermissionState("anvil.kick"));
        assertEquals(PermissionState.TRUE, user.getDirectPermissionState("loom.chat.color"));
    }

    @Test
    void equalityByUuid() {
        UUID uuid = UUID.randomUUID();
        User a = new User(uuid, "Name1");
        User b = new User(uuid, "Name2");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityByUuid() {
        User a = new User(UUID.randomUUID(), "Colsson");
        User b = new User(UUID.randomUUID(), "Colsson");

        assertNotEquals(a, b);
    }

    @Test
    void rejectNullGroup() {
        assertThrows(IllegalArgumentException.class,
            () -> user.addGroup(null));
    }
}
