package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;
import com.colsson.beacon.model.*;

import java.util.*;

/**
 * /beacon group [list|create] | /beacon group <nombre> [...]
 * Maneja todo el enrutamiento internamente porque el primer arg
 * puede ser "list"/"create" (subcomando fijo) o un nombre de grupo (dinámico).
 */
public class GroupCommandHandler implements CommandHandler {

    @Override
    public void execute(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length == 0) {
            ctx.reply(MessageHelper.error("Uso: /beacon group list|create|<nombre>"));
            return;
        }

        String first = args[0].toLowerCase();

        // Top-level subcommands
        switch (first) {
            case "list" -> handleList(api, ctx);
            case "create" -> handleCreate(api, ctx, Arrays.copyOfRange(args, 1, args.length));
            default -> handleGroupNamed(api, ctx, args[0], Arrays.copyOfRange(args, 1, args.length));
        }
    }

    // ── Gestión ─────────────────────────────────────────────

    private void handleList(BeaconAPI api, CommandContext ctx) {
        List<Group> groups = api.getGroups();
        if (groups.isEmpty()) {
            ctx.reply(MessageHelper.info("No hay grupos creados."));
            return;
        }

        ctx.reply(MessageHelper.header("Grupos (" + groups.size() + ")"));
        for (Group g : groups) {
            ctx.reply("§f" + g.name() +
                       " §7(prioridad: " + g.priority() +
                       ", permisos: " + g.permissions().size() +
                       ", padres: " + g.parents().size() + ")");
        }
    }

    private void handleCreate(BeaconAPI api, CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error("Uso: /beacon group create <nombre> [razón]"));
            return;
        }
        String name = args[0];
        String reason = args.length > 1 ? args[1] : null;

        try {
            long id = api.createGroup(name, 0, "", ctx.senderName(), reason);
            ctx.reply(MessageHelper.success("Grupo '" + name + "' creado (id: " + id + ")."));
        } catch (Exception e) {
            ctx.reply(MessageHelper.error(e.getMessage()));
        }
    }

    private void handleGroupNamed(BeaconAPI api, CommandContext ctx,
                                   String groupName, String[] args) {
        if (args.length == 0) {
            ctx.reply(MessageHelper.error(
                "Uso: /beacon group " + groupName + " info|delete|edit|parents|parent|permission"));
            return;
        }

        String subcommand = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (subcommand) {
            case "info" -> handleInfo(api, ctx, groupName);
            case "delete" -> handleDelete(api, ctx, groupName, rest);
            case "parents" -> handleParents(api, ctx, groupName);
            case "edit" -> handleEdit(api, ctx, groupName, rest);
            case "parent" -> handleParent(api, ctx, groupName, rest);
            case "permission" -> handlePermission(api, ctx, groupName, rest);
            default -> ctx.reply(MessageHelper.error(
                "Subcomando desconocido: '" + subcommand + "'."));
        }
    }

    private void handleInfo(BeaconAPI api, CommandContext ctx, String groupName) {
        Optional<Group> group = api.getGroup(groupName);
        if (group.isEmpty()) {
            ctx.reply(MessageHelper.error("Grupo '" + groupName + "' no encontrado."));
            return;
        }

        Group g = group.get();
        ctx.reply(MessageHelper.header("Info: " + g.name()));
        ctx.reply(MessageHelper.keyValue("ID", String.valueOf(g.id())));
        ctx.reply(MessageHelper.keyValue("Prioridad", String.valueOf(g.priority())));
        ctx.reply(MessageHelper.keyValue("Descripción",
            g.description().isEmpty() ? "(sin descripción)" : g.description()));
        ctx.reply(MessageHelper.keyValue("Permisos", String.valueOf(g.permissions().size())));
        ctx.reply(MessageHelper.keyValue("Padres", String.valueOf(g.parents().size())));
        ctx.reply(MessageHelper.keyValue("Hijos", String.valueOf(g.children().size())));
        ctx.reply(MessageHelper.keyValue("Ancestros", String.valueOf(g.ancestors().size())));
    }

    private void handleDelete(BeaconAPI api, CommandContext ctx,
                               String groupName, String[] args) {
        String reason = args.length > 0 ? args[0] : null;

        try {
            api.deleteGroup(groupName, ctx.senderName(), reason);
            ctx.reply(MessageHelper.success("Grupo '" + groupName + "' eliminado."));
        } catch (Exception e) {
            ctx.reply(MessageHelper.error(e.getMessage()));
        }
    }

    private void handleParents(BeaconAPI api, CommandContext ctx, String groupName) {
        List<Group> parents = api.getParents(groupName);
        if (parents.isEmpty()) {
            ctx.reply(MessageHelper.info("El grupo '" + groupName + "' no tiene padres."));
            return;
        }

        ctx.reply(MessageHelper.header("Padres: " + groupName));
        for (Group p : parents) {
            ctx.reply("§f" + p.name() + " §7(prioridad: " + p.priority() + ")");
        }
    }

    // ── Edición ─────────────────────────────────────────────

    private void handleEdit(BeaconAPI api, CommandContext ctx,
                             String groupName, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error(
                "Uso: /beacon group " + groupName + " edit name|priority|description <valor> [razón]"));
            return;
        }

        String field = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (field) {
            case "name" -> {
                if (rest.length < 1) {
                    ctx.reply(MessageHelper.error("Uso: /beacon group " + groupName +
                                                  " edit name <nuevoNombre> [razón]"));
                    return;
                }
                String newName = rest[0];
                String reason = rest.length > 1 ? rest[1] : null;
                try {
                    api.renameGroup(groupName, newName, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success("Grupo renombrado: " + groupName + " → " + newName));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "priority" -> {
                if (rest.length < 1) {
                    ctx.reply(MessageHelper.error("Uso: /beacon group " + groupName +
                                                  " edit priority <numero> [razón]"));
                    return;
                }
                int priority;
                try {
                    priority = Integer.parseInt(rest[0]);
                } catch (NumberFormatException e) {
                    ctx.reply(MessageHelper.error("Prioridad inválida: '" + rest[0] + "'."));
                    return;
                }
                String reason = rest.length > 1 ? rest[1] : null;
                try {
                    api.setGroupPriority(groupName, priority, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success("Prioridad de '" + groupName + "' = " + priority));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "description" -> {
                if (rest.length < 1) {
                    ctx.reply(MessageHelper.error("Uso: /beacon group " + groupName +
                                                  " edit description <texto> [razón]"));
                    return;
                }
                String description = rest[0];
                String reason = rest.length > 1 ? rest[1] : null;
                try {
                    api.setGroupDescription(groupName, description, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success("Descripción de '" + groupName + "' actualizada."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            default -> ctx.reply(MessageHelper.error(
                "Campo desconocido: '" + field + "'. Usa name, priority o description."));
        }
    }

    // ── Herencia ────────────────────────────────────────────

    private void handleParent(BeaconAPI api, CommandContext ctx,
                               String groupName, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error(
                "Uso: /beacon group " + groupName + " parent set|remove <padre> [razón]"));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "set" -> {
                if (args.length < 2) {
                    ctx.reply(MessageHelper.error("Uso: /beacon group " + groupName +
                                                  " parent set <padre> [razón]"));
                    return;
                }
                String parentName = args[1];
                String reason = args.length > 2 ? args[2] : null;
                try {
                    api.setParent(groupName, parentName, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        groupName + " ahora hereda de " + parentName + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    ctx.reply(MessageHelper.error("Uso: /beacon group " + groupName +
                                                  " parent remove <padre> [razón]"));
                    return;
                }
                String parentName = args[1];
                String reason = args.length > 2 ? args[2] : null;
                try {
                    api.removeParent(groupName, parentName, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        groupName + " ya no hereda de " + parentName + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            default -> ctx.reply(MessageHelper.error(
                "Acción desconocida: '" + action + "'. Usa set o remove."));
        }
    }

    // ── Permisos de grupo ───────────────────────────────────

    private void handlePermission(BeaconAPI api, CommandContext ctx,
                                   String groupName, String[] args) {
        if (args.length < 1) {
            ctx.reply(MessageHelper.error(
                "Uso: /beacon group " + groupName +
                " permission set|remove|clear [permiso] [true|false] [razón]"));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "set" -> {
                if (args.length < 3) {
                    ctx.reply(MessageHelper.error(
                        "Uso: /beacon group " + groupName +
                        " permission set <permiso> <true|false> [razón]"));
                    return;
                }
                String permission = args[1];
                String valueStr = args[2];
                String reason = args.length > 3 ? args[3] : null;

                Boolean value = parseBoolean(valueStr);
                if (value == null) {
                    ctx.reply(MessageHelper.error(
                        "Valor inválido: '" + valueStr + "'. Usa true o false."));
                    return;
                }

                try {
                    api.setGroupPermission(groupName, permission, value, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        "Permiso " + permission + " = " + value + " en grupo " + groupName + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    ctx.reply(MessageHelper.error(
                        "Uso: /beacon group " + groupName +
                        " permission remove <permiso> [razón]"));
                    return;
                }
                String permission = args[1];
                String reason = args.length > 2 ? args[2] : null;

                try {
                    api.removeGroupPermission(groupName, permission, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        "Permiso " + permission + " eliminado de grupo " + groupName + "."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            case "clear" -> {
                String reason = args.length > 1 ? args[1] : null;

                try {
                    api.clearGroupPermissions(groupName, ctx.senderName(), reason);
                    ctx.reply(MessageHelper.success(
                        "Permisos del grupo '" + groupName + "' eliminados."));
                } catch (Exception e) {
                    ctx.reply(MessageHelper.error(e.getMessage()));
                }
            }
            default -> ctx.reply(MessageHelper.error(
                "Acción desconocida: '" + action + "'. Usa set, remove o clear."));
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)) return false;
        return null;
    }
}
