package com.colsson.beacon.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones CRUD de usuarios.
 */
public class UserRepository {

    private final DatabaseManager db;

    public UserRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Crea un usuario en la base de datos.
     */
    public void create(java.util.UUID uuid, String username) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT INTO users (uuid, username) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza el nombre de usuario.
     */
    public void updateUsername(java.util.UUID uuid, String newUsername) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "UPDATE users SET username = ? WHERE uuid = ?")) {
            ps.setString(1, newUsername);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    /**
     * Elimina un usuario.
     */
    public void delete(java.util.UUID uuid) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement("DELETE FROM users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    /**
     * Busca un usuario por UUID.
     */
    public Optional<UserRecord> findByUuid(java.util.UUID uuid) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT uuid, username FROM users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserRecord(
                        java.util.UUID.fromString(rs.getString("uuid")),
                        rs.getString("username")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Busca un usuario por nombre.
     */
    public Optional<UserRecord> findByName(String username) throws SQLException {
        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "SELECT uuid, username FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserRecord(
                        java.util.UUID.fromString(rs.getString("uuid")),
                        rs.getString("username")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retorna todos los usuarios.
     */
    public List<UserRecord> findAll() throws SQLException {
        List<UserRecord> users = new java.util.ArrayList<>();
        try (Connection conn = db.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT uuid, username FROM users")) {
            while (rs.next()) {
                users.add(new UserRecord(
                    java.util.UUID.fromString(rs.getString("uuid")),
                    rs.getString("username")
                ));
            }
        }
        return users;
    }

    public record UserRecord(java.util.UUID uuid, String username) {}
}
