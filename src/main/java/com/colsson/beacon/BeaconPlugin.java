package com.colsson.beacon;

import com.colsson.beacon.api.BeaconAPIImpl;
import com.colsson.beacon.cache.CacheManager;
import com.colsson.beacon.commands.BeaconCommandRouter;
import com.colsson.beacon.config.BeaconConfig;
import com.colsson.beacon.persistence.*;
import com.colsson.beacon.resolver.PermissionResolver;

import java.util.logging.Logger;

/**
 * Lógica central de Beacon (POJO testeable).
 *
 * <p>Sin dependencia Paper. En producción, {@link BeaconJavaPlugin}
 * extiende JavaPlugin y delega a esta clase.
 *
 * <p>Ciclo de vida:
 * <pre>
 * 1. Configuración (config.yml)
 * 2. Base de datos (migraciones)
 * 3. Repositorios
 * 4. Cache
 * 5. API
 * 6. Comandos
 * </pre>
 */
public class BeaconPlugin {

    private final Logger logger;
    private BeaconConfig config;
    private DatabaseManager db;
    private BeaconAPIImpl api;
    private BeaconCommandRouter commandRouter;
    private boolean enabled = false;

    public BeaconPlugin(Logger logger) {
        this.logger = logger;
    }

    /**
     * Inicializa el plugin con la configuración dada.
     * POJO testeable sin dependencia Paper.
     */
    public void enable(BeaconConfig config) {
        this.config = config;
        logger.info("Iniciando Beacon...");

        // 1. Base de datos
        logger.info("Conectando a MySQL: " + config.getMysqlHost() + ":" +
                     config.getMysqlPort() + "/" + config.getMysqlDatabase());
        db = new DatabaseManager(
            config.getMysqlHost(),
            config.getMysqlPort(),
            config.getMysqlDatabase(),
            config.getMysqlUsername(),
            config.getMysqlPassword()
        );

        // 2. Migraciones
        if (config.isAutoMigrate()) {
            logger.info("Ejecutando migraciones...");
            try {
                var migManager = new MigrationManager(db);
                migManager.migrate(java.util.Arrays.asList(getMigrations()));
                logger.info("Migraciones completadas.");
            } catch (Exception e) {
                logger.severe("Error en migraciones: " + e.getMessage());
                return;
            }
        }

        // 3. Repositorios
        var userRepo = new UserRepository(db);
        var groupRepo = new GroupRepository(db);
        var permRepo = new PermissionRepository(db);
        var inhRepo = new InheritanceRepository(db);
        var auditRepo = new AuditRepository(db);

        // 4. Cache
        CacheManager cache = new CacheManager(
            config.isCacheEnabled(),
            config.getCacheTtlSeconds(),
            config.getCacheMaxSize()
        );

        // 5. API
        var resolver = new PermissionResolver();
        api = new BeaconAPIImpl(db, userRepo, groupRepo, permRepo, inhRepo,
                                auditRepo, resolver, cache, logger);
        api.setServerName(config.getServerName());

        // 6. Comandos
        commandRouter = new BeaconCommandRouter(api, auditRepo, logger);

        enabled = true;
        logger.info("Beacon habilitado — servidor: " + config.getServerName());
    }

    /**
     * Deshabilita el plugin.
     */
    public void disable() {
        if (db != null) {
            db.close();
        }
        enabled = false;
        logger.info("Beacon deshabilitado.");
    }

    // ── Getters ─────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }
    public BeaconConfig getConfig() { return config; }
    public BeaconAPIImpl getApi() { return api; }
    public BeaconCommandRouter getCommandRouter() { return commandRouter; }
    public DatabaseManager getDatabase() { return db; }

    // ── Migraciones ─────────────────────────────────────────

    private String[] getMigrations() {
        return new String[]{
            // V1 — Esquema inicial (SPEC §9)
            """
            CREATE TABLE IF NOT EXISTS users (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS `groups` (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(64) NOT NULL UNIQUE,
                priority INT NOT NULL DEFAULT 0,
                description VARCHAR(255) DEFAULT ''
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS group_permissions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                group_id BIGINT NOT NULL,
                permission VARCHAR(128) NOT NULL,
                value TINYINT(1) NOT NULL,
                FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
                UNIQUE(group_id, permission)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS user_permissions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_uuid VARCHAR(36) NOT NULL,
                permission VARCHAR(128) NOT NULL,
                value TINYINT(1) NOT NULL,
                FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
                UNIQUE(user_uuid, permission)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS user_groups (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_uuid VARCHAR(36) NOT NULL,
                group_id BIGINT NOT NULL,
                FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
                FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
                UNIQUE(user_uuid, group_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS group_inheritance (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                child_id BIGINT NOT NULL,
                parent_id BIGINT NOT NULL,
                FOREIGN KEY (child_id) REFERENCES `groups`(id) ON DELETE CASCADE,
                FOREIGN KEY (parent_id) REFERENCES `groups`(id) ON DELETE CASCADE,
                UNIQUE(child_id, parent_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS audit_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                actor VARCHAR(64) NOT NULL,
                action VARCHAR(32) NOT NULL,
                target_type VARCHAR(32) NOT NULL,
                target VARCHAR(128) NOT NULL,
                old_value VARCHAR(255),
                new_value VARCHAR(255),
                reason VARCHAR(255),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                server VARCHAR(64)
            )
            """,
        };
    }
}
