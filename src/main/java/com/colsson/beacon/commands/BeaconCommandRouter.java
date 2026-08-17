package com.colsson.beacon.commands;

import com.colsson.beacon.api.BeaconAPI;
import com.colsson.beacon.commands.handlers.*;
import com.colsson.beacon.persistence.AuditRepository;

import java.util.logging.Logger;

/**
 * Router principal de comandos de Beacon.
 * Paper-independiente: el adaptador Paper llama a dispatch().
 */
public class BeaconCommandRouter {

    private final CommandRouter root;
    private final BeaconAPI api;

    public BeaconCommandRouter(BeaconAPI api, AuditRepository auditRepo, Logger logger) {
        this.api = api;
        this.root = new CommandRouter(logger);
        registerCommands(auditRepo);
    }

    private void registerCommands(AuditRepository auditRepo) {
        // /beacon help
        root.register("help", new HelpHandler());

        // /beacon info
        root.register("info", new InfoHandler());

        // /beacon reload
        root.register("reload", new ReloadHandler());

        // /beacon user ...
        root.register("user", new UserCommandHandler());

        // /beacon group ...
        root.register("group", new GroupCommandHandler());

        // /beacon permission ...
        root.register("permission", new PermissionCommandHandler());

        // /beacon check, /beacon groups tree, /beacon debug
        new DiagnosticCommandHandler().register(root);

        // /beacon history
        new HistoryCommandHandler(auditRepo).register(root);
    }

    /**
     * Despacha un comando /beacon.
     *
     * @param ctx   contexto del comando
     * @param args  argumentos (sin el "/beacon" inicial)
     */
    public void dispatch(CommandContext ctx, String[] args) {
        root.dispatch(api, ctx, args);
    }

    public CommandRouter getRoot() {
        return root;
    }
}
