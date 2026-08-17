package com.colsson.beacon.persistence;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona migraciones versionadas del esquema.
 *
 * <p>Cada migración se identifica por un número de versión entero.
 * Las migraciones se ejecutan en orden y solo una vez.
 */
public class MigrationManager {

    private final DatabaseManager db;

    public MigrationManager(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Inicializa la tabla de migraciones si no existe.
     */
    public void ensureMigrationsTable() throws SQLException {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version INTEGER PRIMARY KEY,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }

    /**
     * Retorna la versión actual del esquema.
     */
    public int getCurrentVersion() throws SQLException {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_migrations")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Ejecuta todas las migraciones pendientes.
     *
     * @param migrations lista de migraciones en orden (índice 0 = versión 1)
     */
    public void migrate(List<String> migrations) throws SQLException {
        ensureMigrationsTable();
        int current = getCurrentVersion();

        for (int i = current; i < migrations.size(); i++) {
            int version = i + 1;
            String sql = migrations.get(i);

            try (Connection conn = db.getConnection()) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    for (String statement : splitStatements(sql)) {
                        String trimmed = statement.strip();
                        if (!trimmed.isEmpty()) {
                            stmt.executeUpdate(trimmed);
                        }
                    }
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO schema_migrations (version) VALUES (?)")) {
                        insert.setInt(1, version);
                        insert.executeUpdate();
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
        }
    }

    private List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                String stmt = current.toString().strip();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
                continue;
            }
            current.append(c);
        }

        String last = current.toString().strip();
        if (!last.isEmpty()) {
            statements.add(last);
        }

        return statements;
    }
}
