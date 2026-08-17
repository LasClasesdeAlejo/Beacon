package com.colsson.beacon.persistence;

import com.colsson.beacon.model.AuditAction;
import org.junit.jupiter.api.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditRepositoryTest {

    private DatabaseManager db;
    private AuditRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDatabaseHelper.createInMemoryDb();
        TestDatabaseHelper.createSchema(db);
        repo = new AuditRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void createAndFindRecent() throws Exception {
        repo.create("admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
                     null, "created", "Test reason", Instant.now(), "lobby");

        var entries = repo.findRecent(10);
        assertEquals(1, entries.size());

        var entry = entries.getFirst();
        assertEquals("admin", entry.actor());
        assertEquals(AuditAction.GROUP_CREATE, entry.action());
        assertEquals("GROUP", entry.targetType());
        assertEquals("VIP", entry.target());
        assertNull(entry.oldValue());
        assertEquals("created", entry.newValue());
        assertEquals("Test reason", entry.reason());
        assertEquals("lobby", entry.server());
    }

    @Test
    void createWithoutReason() throws Exception {
        repo.create("admin", AuditAction.USER_GROUP_ADD, "USER", "Colsson",
                     null, "VIP", null, Instant.now(), "lobby");

        var entries = repo.findRecent(10);
        assertEquals(1, entries.size());
        assertNull(entries.getFirst().reason());
    }

    @Test
    void findRecentOrderedByDesc() throws Exception {
        repo.create("admin", AuditAction.GROUP_CREATE, "GROUP", "A",
                     null, "created", null, Instant.now(), "lobby");
        repo.create("admin", AuditAction.GROUP_CREATE, "GROUP", "B",
                     null, "created", null, Instant.now(), "lobby");
        repo.create("admin", AuditAction.GROUP_CREATE, "GROUP", "C",
                     null, "created", null, Instant.now(), "lobby");

        var entries = repo.findRecent(2);
        assertEquals(2, entries.size());
        assertEquals("C", entries.getFirst().target());
        assertEquals("B", entries.get(1).target());
    }

    @Test
    void findByTarget() throws Exception {
        repo.create("admin", AuditAction.USER_GROUP_ADD, "USER", "Colsson",
                     null, "VIP", null, Instant.now(), "lobby");
        repo.create("admin", AuditAction.USER_PERMISSION_SET, "USER", "Colsson",
                     null, "TRUE", null, Instant.now(), "lobby");
        repo.create("admin", AuditAction.GROUP_CREATE, "GROUP", "VIP",
                     null, "created", null, Instant.now(), "lobby");

        var entries = repo.findByTarget("USER", "Colsson", 10);
        assertEquals(2, entries.size());
    }

    @Test
    void findByTargetReturnsEmpty() throws Exception {
        assertTrue(repo.findByTarget("USER", "NonExistent", 10).isEmpty());
    }

    @Test
    void auditActionPreserved() throws Exception {
        for (AuditAction action : AuditAction.values()) {
            repo.create("admin", action, "TARGET", "target-" + action.name(),
                         "old", "new", null, Instant.now(), "lobby");
        }

        var entries = repo.findRecent(100);
        assertEquals(AuditAction.values().length, entries.size());
    }
}
