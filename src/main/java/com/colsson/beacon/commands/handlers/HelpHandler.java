package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;

/**
 * /beacon help — Lista todos los comandos disponibles.
 */
public class HelpHandler implements CommandHandler {

    @Override
    public void execute(BeaconAPI api, CommandContext ctx, String[] args) {
        ctx.reply(MessageHelper.header("Beacon — Ayuda"));
        ctx.reply("");
        ctx.reply("§6/beacon info §7- Información del plugin");
        ctx.reply("§6/beacon reload §7- Recarga la configuración");
        ctx.reply("");
        ctx.reply("§6§lUsuarios:");
        ctx.reply("§f/beacon user <jugador> info §7- Info del usuario");
        ctx.reply("§f/beacon user <jugador> groups §7- Grupos del usuario");
        ctx.reply("§f/beacon user <jugador> permissions §7- Permisos del usuario");
        ctx.reply("§f/beacon user <jugador> group add <grupo> [razón]");
        ctx.reply("§f/beacon user <jugador> group remove <grupo> [razón]");
        ctx.reply("§f/beacon user <jugador> permission set <permiso> <true|false> [razón]");
        ctx.reply("§f/beacon user <jugador> permission remove <permiso> [razón]");
        ctx.reply("§f/beacon user <jugador> permission clear [razón]");
        ctx.reply("");
        ctx.reply("§6§lGrupos:");
        ctx.reply("§f/beacon group list §7- Lista todos los grupos");
        ctx.reply("§f/beacon group create <nombre> [razón]");
        ctx.reply("§f/beacon group <nombre> info");
        ctx.reply("§f/beacon group <nombre> delete [razón]");
        ctx.reply("§f/beacon group <nombre> edit name|priority|description <valor> [razón]");
        ctx.reply("§f/beacon group <nombre> parents §7- Padres del grupo");
        ctx.reply("§f/beacon group <nombre> parent set|remove <padre> [razón]");
        ctx.reply("§f/beacon group <nombre> permission set|remove|clear [permiso] [true|false] [razón]");
        ctx.reply("");
        ctx.reply("§6§lPermisos:");
        ctx.reply("§f/beacon permission list §7- Todos los permisos");
        ctx.reply("§f/beacon permission info <permiso> §7- Info de un permiso");
        ctx.reply("§f/beacon permission search <texto> §7- Buscar permisos");
        ctx.reply("");
        ctx.reply("§6§lDiagnóstico:");
        ctx.reply("§f/beacon check <jugador> <permiso> §7- Verificar permiso");
        ctx.reply("§f/beacon groups tree §7- Árbol de grupos");
        ctx.reply("§f/beacon debug [user|permission] [jugador] [permiso]");
        ctx.reply("");
        ctx.reply("§6§lHistorial:");
        ctx.reply("§f/beacon history §7- Últimas 10 entradas");
        ctx.reply("§f/beacon history user|group|permission <targeto>");
    }
}
