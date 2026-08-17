package com.colsson.beacon.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionAssignmentTest {

    @Test
    void createWithTrue() {
        PermissionAssignment a = new PermissionAssignment("anvil.fly", true);
        assertEquals("anvil.fly", a.permission());
        assertTrue(a.value());
        assertEquals(PermissionState.TRUE, a.toState());
    }

    @Test
    void createWithFalse() {
        PermissionAssignment a = new PermissionAssignment("anvil.fly", false);
        assertEquals("anvil.fly", a.permission());
        assertFalse(a.value());
        assertEquals(PermissionState.FALSE, a.toState());
    }

    @Test
    void rejectNullPermission() {
        assertThrows(IllegalArgumentException.class,
            () -> new PermissionAssignment(null, true));
    }

    @Test
    void rejectBlankPermission() {
        assertThrows(IllegalArgumentException.class,
            () -> new PermissionAssignment("  ", true));
    }

    @Test
    void equalityByPermissionAndValue() {
        PermissionAssignment a1 = new PermissionAssignment("anvil.fly", true);
        PermissionAssignment a2 = new PermissionAssignment("anvil.fly", true);
        PermissionAssignment a3 = new PermissionAssignment("anvil.fly", false);
        PermissionAssignment a4 = new PermissionAssignment("anvil.kick", true);

        assertEquals(a1, a2);
        assertNotEquals(a1, a3);
        assertNotEquals(a1, a4);
    }

    @Test
    void toStringRepresentation() {
        PermissionAssignment a = new PermissionAssignment("anvil.fly", true);
        assertEquals("anvil.fly=TRUE", a.toString());

        PermissionAssignment b = new PermissionAssignment("anvil.fly", false);
        assertEquals("anvil.fly=FALSE", b.toString());
    }
}
