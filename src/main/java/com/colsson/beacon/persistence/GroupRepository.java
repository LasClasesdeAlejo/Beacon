package com.colsson.beacon.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones CRUD de grupos.
 */
public class GroupRepository {

    private final DatabaseManager db;

    public GroupRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Crea un grupo y retorna su ID generado.
     */
    public long create(String name, int priority, String description) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO `groups` (name, priority, description) VALUES (?, ?, ?)",
                 java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, priority);
            ps.setString(3, description != null ? description : "");
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    /**
     * Actualiza el nombre de un grupo.
     */
    public void updateName(long id, String newName) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement("UPDATE `groups` SET name = ? WHERE id = ?")) {
            ps.setString(1, newName);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza la prioridad de un grupo.
     */
    public void updatePriority(long id, int newPriority) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement("UPDATE `groups` SET priority = ? WHERE id = ?")) {
            ps.setInt(1, newPriority);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza la descripción de un grupo.
     */
    public void updateDescription(long id, String newDescription) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement("UPDATE `groups` SET description = ? WHERE id = ?")) {
            ps.setString(1, newDescription != null ? newDescription : "");
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina un grupo.
     */
    public void delete(long id) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (var ps1 = conn.prepareStatement("DELETE FROM group_permissions WHERE group_id = ?")) {
                ps1.setLong(1, id);
                ps1.executeUpdate();
            }
            try (var ps2 = conn.prepareStatement("DELETE FROM group_inheritance WHERE child_id = ? OR parent_id = ?")) {
                ps2.setLong(1, id);
                ps2.setLong(2, id);
                ps2.executeUpdate();
            }
            try (var ps3 = conn.prepareStatement("DELETE FROM user_groups WHERE group_id = ?")) {
                ps3.setLong(1, id);
                ps3.executeUpdate();
            }
            try (var ps4 = conn.prepareStatement("DELETE FROM `groups` WHERE id = ?")) {
                ps4.setLong(1, id);
                ps4.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            try (Connection conn = db.getConnection()) { conn.rollback(); }
            throw e;
        }
    }

    /**
     * Busca un grupo por ID.
     */
    public Optional<GroupRecord> findById(long id) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT id, name, priority, description FROM `groups` WHERE id = ?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(recordFromRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Busca un grupo por nombre.
     */
    public Optional<GroupRecord> findByName(String name) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT id, name, priority, description FROM `groups` WHERE name = ?")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(recordFromRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retorna todos los grupos ordenados por prioridad descendente.
     */
    public List<GroupRecord> findAll() throws SQLException {
        List<GroupRecord> groups = new java.util.ArrayList<>();
        try (Connection conn = db.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT id, name, priority, description FROM `groups` ORDER BY priority DESC")) {
            while (rs.next()) {
                groups.add(recordFromRow(rs));
            }
        }
        return groups;
    }

    private GroupRecord recordFromRow(ResultSet rs) throws SQLException {
        return new GroupRecord(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getInt("priority"),
            rs.getString("description")
        );
    }

    public record GroupRecord(long id, String name, int priority, String description) {}
}
