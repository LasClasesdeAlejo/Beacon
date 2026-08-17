package com.colsson.beacon.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionResultTest {

    @Test
    void undefinedResult() {
        PermissionResult result = PermissionResult.undefined();
        assertEquals(PermissionState.UNDEFINED, result.state());
        assertEquals("none", result.source());
        assertTrue(result.isUndefined());
    }

    @Test
    void grantedResult() {
        PermissionResult result = new PermissionResult(
            PermissionState.TRUE, "MVP++", "MVP++ → anvil.fly = TRUE"
        );
        assertTrue(result.isGranted());
        assertFalse(result.isDenied());
        assertFalse(result.isUndefined());
    }

    @Test
    void deniedResult() {
        PermissionResult result = new PermissionResult(
            PermissionState.FALSE, "MOD", "MOD → anvil.fly = FALSE"
        );
        assertFalse(result.isGranted());
        assertTrue(result.isDenied());
        assertFalse(result.isUndefined());
    }

    @Test
    void equalityByStateAndSource() {
        PermissionResult a = new PermissionResult(PermissionState.TRUE, "VIP", "trace");
        PermissionResult b = new PermissionResult(PermissionState.TRUE, "VIP", "other trace");
        assertEquals(a, b);
    }

    @Test
    void inequalityByState() {
        PermissionResult a = new PermissionResult(PermissionState.TRUE, "VIP", "trace");
        PermissionResult b = new PermissionResult(PermissionState.FALSE, "VIP", "trace");
        assertNotEquals(a, b);
    }

    @Test
    void toStringRepresentation() {
        PermissionResult result = new PermissionResult(
            PermissionState.TRUE, "MVP++", "trace"
        );
        assertEquals("TRUE (source: MVP++)", result.toString());
    }
}
