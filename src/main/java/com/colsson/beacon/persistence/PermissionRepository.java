package com.colsson.beacon.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Repositorio para permisos de grupos y usuarios.
 *
 * <p>La ausencia de una fila representa UNDEFINED.
 * Una fila con value=1 representa TRUE, value=0 representa FALSE.
 */
public class PermissionRepository {

    private final DatabaseManager db;

    public PermissionRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Permisos de grupo ───────────────────────────────────

    /**
     * Establece un permiso para un grupo (TRUE o FALSE).
     * Si ya existe, actualiza el valor.
     */
    public void setGroupPermission(long groupId, String permission, boolean value) throws SQLException {
        try (Connection conn = db.getConnection()) {
            try (var del = conn.prepareStatement(
                    "DELETE FROM group_permissions WHERE group_id = ? AND permission = ?")) {
                del.setLong(1, groupId);
                del.setString(2, permission);
                del.executeUpdate();
            }
            try (var ins = conn.prepareStatement(
                    "INSERT INTO group_permissions (group_id, permission, value) VALUES (?, ?, ?)")) {
                ins.setLong(1, groupId);
                ins.setString(2, permission);
                ins.setInt(3, value ? 1 : 0);
                ins.executeUpdate();
            }
        }
    }

    /**
     * Elimina un permiso de grupo (restaura UNDEFINED).
     */
    public boolean removeGroupPermission(long groupId, String permission) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "DELETE FROM group_permissions WHERE group_id = ? AND permission = ?")) {
            ps.setLong(1, groupId);
            ps.setString(2, permission);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Elimina todos los permisos de un grupo.
     */
    public void clearGroupPermissions(long groupId) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement("DELETE FROM group_permissions WHERE group_id = ?")) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }
    }

    /**
     * Retorna todos los permisos de un grupo.
     */
    public Map<String, Boolean> getGroupPermissions(long groupId) throws SQLException {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT permission, value FROM group_permissions WHERE group_id = ?")) {
            ps.setLong(1, groupId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.put(rs.getString("permission"), rs.getInt("value") == 1);
                }
            }
        }
        return permissions;
    }

    // ── Permisos de usuario ─────────────────────────────────

    /**
     * Establece un permiso directo para un usuario.
     * Si ya existe, actualiza el valor.
     */
    public void setUserPermission(UUID userUuid, String permission, boolean value) throws SQLException {
        try (Connection conn = db.getConnection()) {
            try (var del = conn.prepareStatement(
                    "DELETE FROM user_permissions WHERE user_uuid = ? AND permission = ?")) {
                del.setString(1, userUuid.toString());
                del.setString(2, permission);
                del.executeUpdate();
            }
            try (var ins = conn.prepareStatement(
                    "INSERT INTO user_permissions (user_uuid, permission, value) VALUES (?, ?, ?)")) {
                ins.setString(1, userUuid.toString());
                ins.setString(2, permission);
                ins.setInt(3, value ? 1 : 0);
                ins.executeUpdate();
            }
        }
    }

    /**
     * Elimina un permiso directo de un usuario (restaura UNDEFINED).
     */
    public boolean removeUserPermission(UUID userUuid, String permission) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "DELETE FROM user_permissions WHERE user_uuid = ? AND permission = ?")) {
            ps.setString(1, userUuid.toString());
            ps.setString(2, permission);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Elimina todos los permisos directos de un usuario.
     */
    public void clearUserPermissions(UUID userUuid) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "DELETE FROM user_permissions WHERE user_uuid = ?")) {
            ps.setString(1, userUuid.toString());
            ps.executeUpdate();
        }
    }

    /**
     * Retorna todos los permisos directos de un usuario.
     */
    public Map<String, Boolean> getUserPermissions(UUID userUuid) throws SQLException {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT permission, value FROM user_permissions WHERE user_uuid = ?")) {
            ps.setString(1, userUuid.toString());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.put(rs.getString("permission"), rs.getInt("value") == 1);
                }
            }
        }
        return permissions;
    }

    // ── Consultas globales ──────────────────────────────────

    /**
     * Retorna todos los permisos distintos asignados en el sistema.
     */
    public Set<String> findAllDistinctPermissions() throws SQLException {
        Set<String> permissions = new TreeSet<>();
        try (Connection conn = db.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT DISTINCT permission FROM group_permissions " +
                 "UNION SELECT DISTINCT permission FROM user_permissions")) {
            while (rs.next()) {
                permissions.add(rs.getString("permission"));
            }
        }
        return permissions;
    }
}
