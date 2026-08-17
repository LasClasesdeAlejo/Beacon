package com.colsson.beacon;

import com.colsson.beacon.config.BeaconConfig;
import com.colsson.beacon.listener.PlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Punto de entrada de Beacon en Paper.
 *
 * <p>Delega toda la lógica a {@link BeaconPlugin} (POJO testeable).
 * En tests, se usa BeaconPlugin directamente sin Paper.
 */
public class BeaconJavaPlugin extends JavaPlugin {

    private BeaconPlugin beacon;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        getLogger().info("Beacon cargado.");
    }

    @Override
    public void onEnable() {
        BeaconConfig config = loadConfig();
        beacon = new BeaconPlugin(getLogger());
        beacon.enable(config);

        if (beacon.isEnabled()) {
            registerCommands();
            registerListeners();
            getLogger().info("Beacon habilitado — servidor: " + config.getServerName());
        } else {
            getLogger().severe("Beacon no pudo habilitarse.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (beacon != null) {
            beacon.disable();
        }
        getLogger().info("Beacon deshabilitado.");
    }

    // ── Configuración ───────────────────────────────────────

    private BeaconConfig loadConfig() {
        var cfg = getConfig();
        BeaconConfig config = new BeaconConfig();
        config.setMysqlHost(cfg.getString("mysql.host", "localhost"));
        config.setMysqlPort(cfg.getInt("mysql.port", 3306));
        config.setMysqlDatabase(cfg.getString("mysql.database", "beacon"));
        config.setMysqlUsername(cfg.getString("mysql.username", "root"));
        config.setMysqlPassword(cfg.getString("mysql.password", ""));
        config.setCacheEnabled(cfg.getBoolean("cache.enabled", true));
        config.setCacheTtlSeconds(cfg.getInt("cache.ttl-seconds", 300));
        config.setCacheMaxSize(cfg.getInt("cache.max-size", 1000));
        config.setServerName(cfg.getString("server-name", "default"));
        config.setAutoMigrate(cfg.getBoolean("auto-migrate", true));
        return config;
    }

    // ── Comandos ────────────────────────────────────────────

    private void registerCommands() {
        var router = beacon.getCommandRouter();
        var executor = new PaperCommandExecutor(router, getServer());

        getCommand("beacon").setExecutor(executor);
        getCommand("beacon").setTabCompleter(executor);
    }

    // ── Listeners ───────────────────────────────────────────

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
            new PlayerJoinListener(beacon.getDatabase(), getLogger()), this);
    }

    // ── Getter ──────────────────────────────────────────────

    public BeaconPlugin getBeacon() { return beacon; }
}
