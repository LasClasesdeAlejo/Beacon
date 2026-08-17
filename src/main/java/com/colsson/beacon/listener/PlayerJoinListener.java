package com.colsson.beacon.listener;

import com.colsson.beacon.persistence.DatabaseManager;
import com.colsson.beacon.persistence.UserRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Registra automáticamente los jugadores en la tabla users al unirse.
 * SPEC §9: usuarios identificados por UUID.
 */
public class PlayerJoinListener implements Listener {

    private final DatabaseManager db;
    private final Logger logger;

    public PlayerJoinListener(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String name = event.getPlayer().getName();

        try (Connection conn = db.getConnection();
             var ps = conn.prepareStatement(
                 "INSERT IGNORE INTO users (uuid, username) VALUES (?, ?)")) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error registering player " + name + ": " + e.getMessage());
        }
    }
}
