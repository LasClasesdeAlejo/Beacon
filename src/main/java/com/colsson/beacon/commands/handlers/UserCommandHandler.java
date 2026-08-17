package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;
import com.colsson.beacon.model.*;

import java.util.*;

/**
 * /beacon user <jugador> [info|groups|permissions|group|permission]
 * Maneja todo el enrutamiento internamente porque el primer arg
 * es un nombre de jugador (dinámico), no un subcomando fijo.
 *
 * <p>El primer arg acepta nombre de jugador O UUID.
 */
public class UserCommandHandler implements CommandHandler {

    @Override
    public void execute(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.reply(MessageHelper.error("Uso: /beacon user <jugador> <subcomando>"));
            return;
        }

        String playerName = args[0];
        String subcommand = args[1].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 2, args.length);

        UUID uuid = resolveUuid(api, playerName);
        if (uuid == null) {
            ctx.reply(MessageHelper.error("Jugador '" + playerName + "' no encontrado."));
            return;
        }

        switch (subcommand) {
            case "info" -> handleInfo(api, ctx, uuid, playerName);
            case "groups" -> handleGroups(api, ctx, uuid, playerName);
            case "permissions" -> handlePermissions(api, ctx, uuid, playerName);
            case "group" -> handleGroup(api, ctx, uuid, playerName, rest);
            case "permission" -> handlePermission(api, ctx, uuid, playerName, rest);
            default -> ctx.reply(MessageHelper.error(
                "Subcomando desconocido: '" + subcommand + "'. " +
                "Usa info, groups, permissions, group, permission."));
        }
    }

    // ── Consultas ───────────────────────────────────────────

    private void handleInfo(BeaconAPI api, CommandContext ctx, UUID uuid, String name) {
        Optional<User> user = api.getUser(uuid);
        if (user.isEmpty()) {
            ctx.reply(MessageHelper.error("Usuario '" + name + "' no registrado en Beacon."));
            return;
        }

        User u = user.get();
        ctx.reply(MessageHelper.header("Info: " + u.username()));
        ctx.reply(MessageHelper.keyValue("UUID", u.uuid().toString()));
        ctx.reply(MessageHelper.keyValue("Grupos", String.valueOf(u.groups().size())));
        for (Group g : u.groups()) {
            ctx.reply("  §7- §f" + g.name() + " §7(prioridad: " + g.priority() + ")");
        }
        ctx.reply(MessageHelper.keyValue("Permisos directos",
                                         String.valueOf(u.directPermissions().size())));
    }

    private void handleGroups(BeaconAPI api, CommandContext ctx, UUID uuid, String name) {
        Optional<User> user = api.getUser(uuid);
        if (user.isEmpty()) {
            ctx.reply(MessageHelper.error("Usuario '" + name + "' no registrado."));
            return;
        }

        Set<Group> groups = user.get().groups();
        if (groups.isEmpty()) {
            ctx.reply(MessageHelper.info(name + " no tiene grupos."));
            return;
        }

        ctx.reply(MessageHelper.header("Grupos: " + name));
        for (Group g : groups) {
            ctx.reply("§f" + g.name() + " §7(prioridad: " + g.priority() +
                       ", permisos: " + g.permissions().size() + ")");
        }
    }

    private void handlePermissions(BeaconAPI api, CommandContext ctx, UUID uuid, String name) {
        Optional<User> user = api.getUser(uuid);
        if (user.isEmpty()) {
            ctx.reply(MessageHelper.error("Usuario '" + name + "' no registrado."));
            return;
        }

        Map<String, PermissionAssignment> perms = user.get().directPermissions();
        if (perms.isEmpty()) {
            ctx.reply(MessageHelper.info(name + " no tiene permisos directos."));
            return;
        }

        ctx.reply(MessageHelper.header("Permisos directos: " + name));
        for (var entry : perms.entrySet()) {
            String color = entry.getValue().value() ? "§a" : "§c";
            ctx.reply(color + entry.getKey() + " §7= " +
                       (entry.getValue().value() ? "§aTRUE" : "§cFALSE"));
        }
    }

    // ── Modificaciones — Grupos ─────────────────────────────

    private void handleGroup(BeaconAPI api, CommandContext ctx, UUID uuid,
                             String name, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error("Uso: /beacon user <jugador> group add|remove <grupo> [razón]"));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "add" -> {
                if (args.length < 2) {
                    ctx.reply(MessageHelper.error("Uso: /beacon user <jugador> group add <grupo> [razón]"));
                    return;
                }
                String groupName = args[1];
                String reason = args.length > 2 ? args[2] : null;
                try {
                    api.addUserToGroup(uuid, groupName, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(name + " añadido al grupo " + groupName + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    ctx.reply(MessageHelper.error("Uso: /beacon user <jugador> group remove <grupo> [razón]"));
                    return;
                }
                String groupName = args[1];
                String reason = args.length > 2 ? args[2] : null;
                try {
                    api.removeUserFromGroup(uuid, groupName, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(name + " eliminado del grupo " + groupName + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            default -> ctx.reply(MessageHelper.error("Acción desconocida: '" + action + "'. Usa add o remove."));
        }
    }

    // ── Modificaciones — Permisos ───────────────────────────

    private void handlePermission(BeaconAPI api, CommandContext ctx, UUID uuid,
                                  String name, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error(
                "Uso: /beacon user <jugador> permission set|remove|clear [permiso] [true|false] [razón]"));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "set" -> {
                if (args.length < 3) {
                    ctx.reply(MessageHelper.error(
                        "Uso: /beacon user <jugador> permission set <permiso> <true|false> [razón]"));
                    return;
                }
                String permission = args[1];
                String valueStr = args[2];
                String reason = args.length > 3 ? args[3] : null;

                Boolean value = parseBoolean(valueStr);
                if (value == null) {
                    ctx.reply(MessageHelper.error("Valor inválido: '" + valueStr + "'. Usa true o false."));
                    return;
                }

                try {
                    api.setUserPermission(uuid, permission, value, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        "Permiso " + permission + " = " + value + " para " + name + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    ctx.reply(MessageHelper.error(
                        "Uso: /beacon user <jugador> permission remove <permiso> [razón]"));
                    return;
                }
                String permission = args[1];
                String reason = args.length > 2 ? args[2] : null;

                try {
                    api.removeUserPermission(uuid, permission, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        "Permiso " + permission + " eliminado de " + name + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "clear" -> {
                String reason = args.length > 1 ? args[1] : null;

                try {
                    api.clearUserPermissions(uuid, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        "Permisos directos de " + name + " eliminados."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            default -> ctx.reply(MessageHelper.error(
                "Acción desconocida: '" + action + "'. Usa set, remove o clear."));
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private UUID resolveUuid(BeaconAPI api, String nameOrUuid) {
        // 1. Intentar como UUID
        try {
            return UUID.fromString(nameOrUuid);
        } catch (IllegalArgumentException ignored) {}

        // 2. Buscar por nombre en la API
        Optional<User> user = api.getUserByName(nameOrUuid);
        return user.map(User::uuid).orElse(null);
    }

    private Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)) return false;
        return null;
    }
}
