package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;
import com.colsson.beacon.model.AuditAction;

import java.util.UUID;

/**
 * /beacon history [user|group|permission] [targeto]
 */
public class HistoryCommandHandler {

    private final com.colsson.beacon.persistence.AuditRepository auditRepo;

    public HistoryCommandHandler(com.colsson.beacon.persistence.AuditRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public void register(CommandRouter router) {
        router.register("history", this::handle);
    }

    private void handle(BeaconAPI api, CommandContext ctx, String[] args) {
        try {
            if (args.length == 0) {
                showRecent(ctx, 10);
            } else if (args[0].equalsIgnoreCase("user") && args.length >= 2) {
                showByTarget(ctx, "USER", args[1], 10);
            } else if (args[0].equalsIgnoreCase("group") && args.length >= 2) {
                showByTarget(ctx, "GROUP", args[1], 10);
            } else if (args[0].equalsIgnoreCase("permission") && args.length >= 2) {
                showByTarget(ctx, "PERMISSION", args[1], 10);
            } else {
                ctx.reply(MessageHelper.error(
                    "Uso: /beacon history [user|group|permission] <targeto>"));
            }
        } catch (Exception e) {
            ctx.reply(MessageHelper.error("Error al consultar historial: " + e.getMessage()));
        }
    }

    private void showRecent(CommandContext ctx, int limit) throws java.sql.SQLException {
        var entries = auditRepo.findRecent(limit);
        if (entries.isEmpty()) {
            ctx.reply(MessageHelper.info("No hay entradas de auditoría."));
            return;
        }

        ctx.reply(MessageHelper.header("Historial reciente (" + entries.size() + ")"));
        for (var entry : entries) {
            ctx.reply("§8[#" + entry.id() + "] §7" +
                       entry.timestamp() + " §f" + entry.actor() +
                       " §7→ " + formatAction(entry.action()) +
                       " §7en " + entry.targetType() + " §f" + entry.target());
            if (entry.oldValue() != null || entry.newValue() != null) {
                ctx.reply("  §8" + entry.oldValue() + " → " + entry.newValue());
            }
            if (entry.reason() != null) {
                ctx.reply("  §7Razón: §f" + entry.reason());
            }
        }
    }

    private void showByTarget(CommandContext ctx, String targetType,
                               String target, int limit) throws java.sql.SQLException {
        var entries = auditRepo.findByTarget(targetType, target, limit);
        if (entries.isEmpty()) {
            ctx.reply(MessageHelper.info("No hay entradas para " + targetType + " '" + target + "'."));
            return;
        }

        ctx.reply(MessageHelper.header("Historial: " + targetType + " " + target +
                                        " (" + entries.size() + ")"));
        for (var entry : entries) {
            ctx.reply("§8[#" + entry.id() + "] §7" +
                       entry.timestamp() + " §f" + entry.actor() +
                       " §7→ " + formatAction(entry.action()));
            if (entry.oldValue() != null || entry.newValue() != null) {
                ctx.reply("  §8" + entry.oldValue() + " → " + entry.newValue());
            }
            if (entry.reason() != null) {
                ctx.reply("  §7Razón: §f" + entry.reason());
            }
        }
    }

    private String formatAction(AuditAction action) {
        return switch (action) {
            case GROUP_CREATE -> "§agroup create";
            case GROUP_DELETE -> "§cgroup delete";
            case GROUP_RENAME -> "§egroup rename";
            case GROUP_EDIT_PRIORITY -> "§egroup edit priority";
            case GROUP_EDIT_DESCRIPTION -> "§egroup edit description";
            case GROUP_PARENT_SET -> "§agroup parent set";
            case GROUP_PARENT_REMOVE -> "§cgroup parent remove";
            case GROUP_PERMISSION_SET -> "§agroup permission set";
            case GROUP_PERMISSION_REMOVE -> "§cgroup permission remove";
            case GROUP_PERMISSION_CLEAR -> "§cgroup permission clear";
            case USER_GROUP_ADD -> "§auser group add";
            case USER_GROUP_REMOVE -> "§cuser group remove";
            case USER_PERMISSION_SET -> "§auser permission set";
            case USER_PERMISSION_REMOVE -> "§cuser permission remove";
            case USER_PERMISSION_CLEAR -> "§cuser permission clear";
        };
    }
}
