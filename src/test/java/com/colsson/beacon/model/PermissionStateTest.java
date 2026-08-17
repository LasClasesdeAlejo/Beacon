package com.colsson.beacon.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PermissionStateTest {

    @Test
    void trueIsDefined() {
        assertTrue(PermissionState.TRUE.isDefined());
        assertTrue(PermissionState.TRUE.isGranted());
    }

    @Test
    void falseIsDefined() {
        assertTrue(PermissionState.FALSE.isDefined());
        assertTrue(PermissionState.FALSE.isDenied());
    }

    @Test
    void undefinedIsNotDefined() {
        assertFalse(PermissionState.UNDEFINED.isDefined());
        assertFalse(PermissionState.UNDEFINED.isGranted());
        assertFalse(PermissionState.UNDEFINED.isDenied());
    }
}
