package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;

import java.util.Arrays;

/**
 * /beacon permission [list|info|search]
 */
public class PermissionCommandHandler implements CommandHandler {

    @Override
    public void execute(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length == 0) {
            ctx.reply(MessageHelper.error("Uso: /beacon permission list|info|search"));
            return;
        }

        String subcommand = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (subcommand) {
            case "list" -> handleList(api, ctx);
            case "info" -> handleInfo(api, ctx, rest);
            case "search" -> handleSearch(api, ctx, rest);
            default -> ctx.reply(MessageHelper.error(
                "Subcomando desconocido: '" + subcommand + "'. Usa list, info o search."));
        }
    }

    private void handleList(BeaconAPI api, CommandContext ctx) {
        ctx.reply(MessageHelper.header("Permisos en el sistema"));
        ctx.reply("§7Usa §f/beacon permission search <texto> §7para buscar.");
    }

    private void handleInfo(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error("Uso: /beacon permission info <permiso>"));
            return;
        }
        String permission = args[0];
        ctx.reply(MessageHelper.header("Permiso: " + permission));
        ctx.reply("§7Consulta con §f/beacon check <jugador> " + permission);
    }

    private void handleSearch(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error("Uso: /beacon permission search <texto>"));
            return;
        }
        String query = args[0].toLowerCase();
        ctx.reply(MessageHelper.header("Búsqueda: " + query));
        ctx.reply("§7Funcionalidad completa disponible con conexión a MySQL.");
    }
}
