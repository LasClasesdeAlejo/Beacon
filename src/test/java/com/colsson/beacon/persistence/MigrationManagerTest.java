package com.colsson.beacon.persistence;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationManagerTest {

    private DatabaseManager db;
    private MigrationManager mgr;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDatabaseHelper.createInMemoryDb();
        mgr = new MigrationManager(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void ensureMigrationsTableCreatesTable() throws Exception {
        mgr.ensureMigrationsTable();

        try (var conn = db.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='schema_migrations'")) {
            assertTrue(rs.next());
        }
    }

    @Test
    void getCurrentVersionReturnsZeroOnEmpty() throws Exception {
        mgr.ensureMigrationsTable();
        assertEquals(0, mgr.getCurrentVersion());
    }

    @Test
    void migrateRunsSingleMigration() throws Exception {
        mgr.migrate(List.of("CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT)"));

        assertEquals(1, mgr.getCurrentVersion());

        try (var conn = db.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='test_table'")) {
            assertTrue(rs.next());
        }
    }

    @Test
    void migrateRunsMultipleMigrations() throws Exception {
        mgr.migrate(List.of(
            "CREATE TABLE t1 (id INTEGER PRIMARY KEY)",
            "CREATE TABLE t2 (id INTEGER PRIMARY KEY)"
        ));

        assertEquals(2, mgr.getCurrentVersion());
    }

    @Test
    void migrateSkipsAlreadyApplied() throws Exception {
        mgr.migrate(List.of("CREATE TABLE t1 (id INTEGER PRIMARY KEY)"));
        mgr.migrate(List.of(
            "CREATE TABLE t1 (id INTEGER PRIMARY KEY)",
            "CREATE TABLE t2 (id INTEGER PRIMARY KEY)"
        ));

        assertEquals(2, mgr.getCurrentVersion());
    }

    @Test
    void migrateHandlesMultipleStatementsPerVersion() throws Exception {
        mgr.migrate(List.of(
            "CREATE TABLE t1 (id INTEGER PRIMARY KEY); CREATE TABLE t2 (id INTEGER PRIMARY KEY)"
        ));

        assertEquals(1, mgr.getCurrentVersion());

        try (var conn = db.getConnection();
             var stmt = conn.createStatement()) {
            var rs1 = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='t1'");
            assertTrue(rs1.next());
            var rs2 = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='t2'");
            assertTrue(rs2.next());
        }
    }
}
