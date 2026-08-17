package com.colsson.beacon.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Helper para tests de persistencia con SQLite embebido.
 */
public class TestDatabaseHelper {

    /**
     * Crea un DatabaseManager con SQLite en memoria.
     */
    public static DatabaseManager createInMemoryDb() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite::memory:");
        config.setMaximumPoolSize(1);
        config.setPoolName("TestPool");
        HikariDataSource ds = new HikariDataSource(config);
        return new DatabaseManager(ds);
    }

    /**
     * Crea las tablas del esquema V1 compatible con SQLite.
     */
    public static void createSchema(DatabaseManager db) throws Exception {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    uuid TEXT PRIMARY KEY,
                    username TEXT NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    priority INTEGER NOT NULL DEFAULT 0,
                    description TEXT DEFAULT ''
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS group_permissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    permission TEXT NOT NULL,
                    value INTEGER NOT NULL,
                    world TEXT DEFAULT NULL,
                    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                    UNIQUE(group_id, permission, world)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_permissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_uuid TEXT NOT NULL,
                    permission TEXT NOT NULL,
                    value INTEGER NOT NULL,
                    world TEXT DEFAULT NULL,
                    FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
                    UNIQUE(user_uuid, permission, world)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_uuid TEXT NOT NULL,
                    group_id INTEGER NOT NULL,
                    FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                    UNIQUE(user_uuid, group_id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS group_inheritance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    child_id INTEGER NOT NULL,
                    parent_id INTEGER NOT NULL,
                    FOREIGN KEY (child_id) REFERENCES groups(id) ON DELETE CASCADE,
                    FOREIGN KEY (parent_id) REFERENCES groups(id) ON DELETE CASCADE,
                    UNIQUE(child_id, parent_id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    actor TEXT NOT NULL,
                    action TEXT NOT NULL,
                    target_type TEXT NOT NULL,
                    target TEXT NOT NULL,
                    old_value TEXT,
                    new_value TEXT,
                    reason TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    server TEXT
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version INTEGER PRIMARY KEY,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }
}
