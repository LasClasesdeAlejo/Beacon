package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;
import com.colsson.beacon.model.*;

import java.util.*;

/**
 * /beacon check <jugador> <permiso>
 * /beacon groups tree
 * /beacon debug [user|permission] [jugador] [permiso]
 */
public class DiagnosticCommandHandler {

    public void register(CommandRouter router) {
        router.register("check", this::handleCheck);
        CommandRouter groupsRouter = router.createSubRouter("groups");
        groupsRouter.register("tree", this::handleGroupsTree);
        router.register("debug", this::handleDebug);
    }

    // /beacon check <jugador> <permiso> [mundo]
    private void handleCheck(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.reply(MessageHelper.error("Uso: /beacon check <jugador> <permiso> [mundo]"));
            return;
        }
        String playerName = args[0];
        String permission = args[1];
        String world = args.length > 2 ? args[2] : null;
        UUID uuid = resolveUuid(playerName);
        if (uuid == null) {
            ctx.reply(MessageHelper.error("Jugador '" + playerName + "' no encontrado."));
            return;
        }

        PermissionResult result = api.getPermissionState(uuid, permission, world);

        ctx.reply(MessageHelper.header("Check: " + playerName + " → " + permission));
        if (world != null) {
            ctx.reply(MessageHelper.keyValue("Mundo", world));
        }
        ctx.reply(MessageHelper.keyValue("Estado", formatState(result.state())));
        ctx.reply(MessageHelper.keyValue("Fuente", result.source()));
        if (result.trace() != null && !result.trace().isBlank()) {
            ctx.reply("§7Traza:");
            for (String line : result.trace().split("\n")) {
                ctx.reply("  §8" + line);
            }
        }
    }

    // /beacon groups tree
    private void handleGroupsTree(BeaconAPI api, CommandContext ctx, String[] args) {
        List<Group> groups = api.getGroups();
        if (groups.isEmpty()) {
            ctx.reply(MessageHelper.info("No hay grupos."));
            return;
        }

        ctx.reply(MessageHelper.header("Árbol de grupos"));

        // Find root groups (no parents)
        List<Group> roots = new ArrayList<>();
        for (Group g : groups) {
            if (g.parents().isEmpty()) {
                roots.add(g);
            }
        }

        if (roots.isEmpty()) {
            // Circular or all groups have parents — just list them
            for (Group g : groups) {
                ctx.reply("§f" + g.name() + " §7(p: " + g.priority() + ")");
            }
            return;
        }

        for (Group root : roots) {
            printTree(api, ctx, root, "", true);
        }
    }

    private void printTree(BeaconAPI api, CommandContext ctx, Group group,
                           String indent, boolean isLast) {
        String connector = isLast ? "└─ " : "├─ ";
        ctx.reply("§7" + indent + connector + "§f" + group.name() +
                   " §8(p:" + group.priority() + ", perm:" + group.permissions().size() + ")");

        String childIndent = indent + (isLast ? "   " : "│  ");
        List<Group> children = new ArrayList<>(group.children());
        for (int i = 0; i < children.size(); i++) {
            printTree(api, ctx, children.get(i), childIndent, i == children.size() - 1);
        }
    }

    // /beacon debug [user|permission] [jugador] [permiso] [mundo]
    private void handleDebug(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length == 0) {
            handleDebugGeneral(api, ctx);
        } else if (args[0].equalsIgnoreCase("user") && args.length >= 2) {
            handleDebugUser(api, ctx, args[1]);
        } else if (args[0].equalsIgnoreCase("permission") && args.length >= 3) {
            String world = args.length > 3 ? args[3] : null;
            handleDebugPermission(api, ctx, args[1], args[2], world);
        } else {
            ctx.reply(MessageHelper.error(
                "Uso: /beacon debug [user <jugador> | permission <jugador> <permiso> [mundo]]"));
        }
    }

    private void handleDebugGeneral(BeaconAPI api, CommandContext ctx) {
        ctx.reply(MessageHelper.header("Debug — Estado del sistema"));
        ctx.reply(MessageHelper.keyValue("Grupos", String.valueOf(api.getGroups().size())));
        ctx.reply(MessageHelper.keyValue("Usuarios", String.valueOf(api.findAllUsers().size())));

        List<Group> groups = api.getGroups();
        for (Group g : groups) {
            ctx.reply("  §7- §f" + g.name() +
                       " §8(id:" + g.id() + ", p:" + g.priority() +
                       ", perm:" + g.permissions().size() +
                       ", parents:" + g.parents().size() +
                       ", children:" + g.children().size() + ")");
        }
    }

    private void handleDebugUser(BeaconAPI api, CommandContext ctx, String playerName) {
        UUID uuid = resolveUuid(playerName);
        if (uuid == null) {
            ctx.reply(MessageHelper.error("Jugador '" + playerName + "' no encontrado."));
            return;
        }

        Optional<User> user = api.getUser(uuid);
        if (user.isEmpty()) {
            ctx.reply(MessageHelper.error("Usuario no registrado en Beacon."));
            return;
        }

        User u = user.get();
        ctx.reply(MessageHelper.header("Debug: " + u.username()));
        ctx.reply(MessageHelper.keyValue("UUID", u.uuid().toString()));
        ctx.reply(MessageHelper.keyValue("Grupos", String.valueOf(u.groups().size())));
        for (Group g : u.groups()) {
            ctx.reply("  §7- §f" + g.name() +
                       " §8(p:" + g.priority() +
                       ", perm:" + g.permissions().size() +
                       ", parents:" + g.parents().size() + ")");
        }
        ctx.reply(MessageHelper.keyValue("Permisos directos",
                                         String.valueOf(u.directPermissions().size())));
        for (var entry : u.directPermissions().entrySet()) {
            String color = entry.getValue().value() ? "§a" : "§c";
            ctx.reply("  §7- " + color + entry.getKey());
        }
    }

    // /beacon debug permission <jugador> <permiso> [mundo]
    private void handleDebugPermission(BeaconAPI api, CommandContext ctx,
                                        String playerName, String permission, String world) {
        UUID uuid = resolveUuid(playerName);
        if (uuid == null) {
            ctx.reply(MessageHelper.error("Jugador '" + playerName + "' no encontrado."));
            return;
        }

        PermissionResult result = api.getPermissionState(uuid, permission, world);

        ctx.reply(MessageHelper.header("Debug: " + playerName + " → " + permission));
        if (world != null) {
            ctx.reply(MessageHelper.keyValue("Mundo", world));
        }
        ctx.reply(MessageHelper.keyValue("Estado", formatState(result.state())));
        ctx.reply(MessageHelper.keyValue("Fuente", result.source()));
        if (result.trace() != null) {
            ctx.reply("§7Traza completa:");
            for (String line : result.trace().split("\n")) {
                ctx.reply("  §8" + line);
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private UUID resolveUuid(String nameOrUuid) {
        try {
            return UUID.fromString(nameOrUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String formatState(PermissionState state) {
        return switch (state) {
            case TRUE -> "§aTRUE";
            case FALSE -> "§cFALSE";
            case UNDEFINED -> "§7UNDEFINED";
        };
    }
}
