package com.colsson.beacon.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Repositorio para relaciones de herencia entre grupos.
 */
public class InheritanceRepository {

    private final DatabaseManager db;

    public InheritanceRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Crea una relación de herencia: child hereda de parent.
     */
    public void addInheritance(long childId, long parentId) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO group_inheritance (child_id, parent_id) VALUES (?, ?)")) {
            ps.setLong(1, childId);
            ps.setLong(2, parentId);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina una relación de herencia.
     */
    public boolean removeInheritance(long childId, long parentId) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "DELETE FROM group_inheritance WHERE child_id = ? AND parent_id = ?")) {
            ps.setLong(1, childId);
            ps.setLong(2, parentId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retorna los IDs de los padres de un grupo.
     */
    public List<Long> getParentIds(long childId) throws SQLException {
        List<Long> parents = new ArrayList<>();
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT parent_id FROM group_inheritance WHERE child_id = ?")) {
            ps.setLong(1, childId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    parents.add(rs.getLong("parent_id"));
                }
            }
        }
        return parents;
    }

    /**
     * Retorna los IDs de los hijos de un grupo.
     */
    public List<Long> getChildIds(long parentId) throws SQLException {
        List<Long> children = new ArrayList<>();
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT child_id FROM group_inheritance WHERE parent_id = ?")) {
            ps.setLong(1, parentId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    children.add(rs.getLong("child_id"));
                }
            }
        }
        return children;
    }

    /**
     * Verifica si existe una relación de herencia.
     */
    public boolean exists(long childId, long parentId) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM group_inheritance WHERE child_id = ? AND parent_id = ?")) {
            ps.setLong(1, childId);
            ps.setLong(2, parentId);
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
