package com.colsson.beacon.config;

/**
 * Configuración de Beacon.
 * POJO puro sin dependencia Paper.
 *
 * <p>En producción, BeaconJavaPlugin lee config.yml
 * y llena este POJO via Bukkit API.
 */
public class BeaconConfig {

    // ── MySQL ───────────────────────────────────────────────

    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "beacon";
    private String mysqlUsername = "root";
    private String mysqlPassword = "";

    // ── Cache ───────────────────────────────────────────────

    private boolean cacheEnabled = true;
    private int cacheTtlSeconds = 300;
    private int cacheMaxSize = 1000;

    // ── Servidor ────────────────────────────────────────────

    private String serverName = "default";

    // ── Migraciones ─────────────────────────────────────────

    private boolean autoMigrate = true;

    // ── Getters / Setters ───────────────────────────────────

    public String getMysqlHost() { return mysqlHost; }
    public void setMysqlHost(String host) { this.mysqlHost = host; }

    public int getMysqlPort() { return mysqlPort; }
    public void setMysqlPort(int port) { this.mysqlPort = port; }

    public String getMysqlDatabase() { return mysqlDatabase; }
    public void setMysqlDatabase(String database) { this.mysqlDatabase = database; }

    public String getMysqlUsername() { return mysqlUsername; }
    public void setMysqlUsername(String username) { this.mysqlUsername = username; }

    public String getMysqlPassword() { return mysqlPassword; }
    public void setMysqlPassword(String password) { this.mysqlPassword = password; }

    public boolean isCacheEnabled() { return cacheEnabled; }
    public void setCacheEnabled(boolean enabled) { this.cacheEnabled = enabled; }

    public int getCacheTtlSeconds() { return cacheTtlSeconds; }
    public void setCacheTtlSeconds(int ttl) { this.cacheTtlSeconds = ttl; }

    public int getCacheMaxSize() { return cacheMaxSize; }
    public void setCacheMaxSize(int maxSize) { this.cacheMaxSize = maxSize; }

    public String getServerName() { return serverName; }
    public void setServerName(String name) { this.serverName = name; }

    public boolean isAutoMigrate() { return autoMigrate; }
    public void setAutoMigrate(boolean auto) { this.autoMigrate = auto; }

    // ── Helpers ─────────────────────────────────────────────

    /**
     * Crea una configuración por defecto.
     */
    public static BeaconConfig defaults() {
        return new BeaconConfig();
    }

    /**
     * Crea una configuración para tests con SQLite en memoria.
     */
    public static BeaconConfig testConfig() {
        BeaconConfig config = new BeaconConfig();
        config.setCacheEnabled(true);
        config.setCacheTtlSeconds(300);
        config.setServerName("test");
        return config;
    }

    @Override
    public String toString() {
        return "BeaconConfig{" +
               "mysql=" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase +
               ", cache=" + (cacheEnabled ? "on(" + cacheTtlSeconds + "s)" : "off") +
               ", server=" + serverName +
               "}";
    }
}
