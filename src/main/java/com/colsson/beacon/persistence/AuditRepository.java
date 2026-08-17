package com.colsson.beacon.persistence;

import com.colsson.beacon.model.AuditAction;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para el registro de auditoría.
 */
public class AuditRepository {

    private final DatabaseManager db;

    public AuditRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Registra una entrada de auditoría.
     */
    public long create(String actor, AuditAction action, String targetType,
                       String target, String oldValue, String newValue,
                       String reason, Instant timestamp, String server) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO audit_log (actor, action, target_type, target, " +
                 "old_value, new_value, reason, timestamp, server) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, actor);
            ps.setString(2, action.name());
            ps.setString(3, targetType);
            ps.setString(4, target);
            ps.setString(5, oldValue);
            ps.setString(6, newValue);
            ps.setString(7, reason);
            ps.setTimestamp(8, Timestamp.from(timestamp));
            ps.setString(9, server);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    /**
     * Retorna las últimas entradas de auditoría.
     */
    public List<AuditEntryRecord> findRecent(int limit) throws SQLException {
        List<AuditEntryRecord> entries = new ArrayList<>();
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT id, actor, action, target_type, target, " +
                 "old_value, new_value, reason, timestamp, server " +
                 "FROM audit_log ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(recordFromRow(rs));
                }
            }
        }
        return entries;
    }

    /**
     * Retorna entradas de auditoría de un usuario específico.
     */
    public List<AuditEntryRecord> findByTarget(String targetType, String target,
                                                int limit) throws SQLException {
        List<AuditEntryRecord> entries = new ArrayList<>();
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT id, actor, action, target_type, target, " +
                 "old_value, new_value, reason, timestamp, server " +
                 "FROM audit_log WHERE target_type = ? AND target = ? " +
                 "ORDER BY id DESC LIMIT ?")) {
            ps.setString(1, targetType);
            ps.setString(2, target);
            ps.setInt(3, limit);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(recordFromRow(rs));
                }
            }
        }
        return entries;
    }

    private AuditEntryRecord recordFromRow(ResultSet rs) throws SQLException {
        return new AuditEntryRecord(
            rs.getLong("id"),
            rs.getString("actor"),
            AuditAction.valueOf(rs.getString("action")),
            rs.getString("target_type"),
            rs.getString("target"),
            rs.getString("old_value"),
            rs.getString("new_value"),
            rs.getString("reason"),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getString("server")
        );
    }

    public record AuditEntryRecord(long id, String actor, AuditAction action,
                                    String targetType, String target,
                                    String oldValue, String newValue,
                                    String reason, Instant timestamp, String server) {}
}
