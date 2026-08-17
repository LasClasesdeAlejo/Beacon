package com.colsson.beacon.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditEntryTest {

    @Test
    void createFullEntry() {
        AuditEntry entry = new AuditEntry(
            1L, "admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
            null, "created", "Test reason", Instant.now(), "lobby"
        );

        assertEquals(1L, entry.id());
        assertEquals("admin", entry.actor());
        assertEquals(AuditAction.GROUP_CREATE, entry.action());
        assertEquals("GROUP", entry.targetType());
        assertEquals("VIP", entry.target());
        assertNull(entry.oldValue());
        assertEquals("created", entry.newValue());
        assertEquals("Test reason", entry.reason());
        assertNotNull(entry.timestamp());
        assertEquals("lobby", entry.server());
    }

    @Test
    void hasReasonReturnsTrueWhenPresent() {
        AuditEntry entry = new AuditEntry(
            1L, "admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
            null, "created", "Reason", Instant.now(), "lobby"
        );
        assertTrue(entry.hasReason());
    }

    @Test
    void hasReasonReturnsFalseWhenNull() {
        AuditEntry entry = new AuditEntry(
            1L, "admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
            null, "created", null, Instant.now(), "lobby"
        );
        assertFalse(entry.hasReason());
    }

    @Test
    void hasReasonReturnsFalseWhenBlank() {
        AuditEntry entry = new AuditEntry(
            1L, "admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
            null, "created", "  ", Instant.now(), "lobby"
        );
        assertFalse(entry.hasReason());
    }

    @Test
    void equalityById() {
        AuditEntry a = new AuditEntry(
            1L, "admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
            null, "created", null, Instant.now(), "lobby"
        );
        AuditEntry b = new AuditEntry(
            1L, "other", AuditAction.GROUP_DELETE, "GROUP", "MOD",
            "created", "deleted", null, Instant.now(), "survival"
        );
        assertEquals(a, b);
    }

    @Test
    void inequalityByDifferentId() {
        AuditEntry a = new AuditEntry(
            1L, "admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
            null, "created", null, Instant.now(), "lobby"
        );
        AuditEntry b = new AuditEntry(
            2L, "admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
            null, "created", null, Instant.now(), "lobby"
        );
        assertNotEquals(a, b);
    }
}
