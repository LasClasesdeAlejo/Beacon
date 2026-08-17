package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;

/**
 * /beacon info — Información del plugin.
 */
public class InfoHandler implements CommandHandler {

    @Override
    public void execute(BeaconAPI api, CommandContext ctx, String[] args) {
        ctx.reply(MessageHelper.header("Beacon Info"));
        ctx.reply(MessageHelper.keyValue("Plugin", "Beacon"));
        ctx.reply(MessageHelper.keyValue("Versión", "1.0.0"));
        ctx.reply(MessageHelper.keyValue("API", "v1"));
        ctx.reply(MessageHelper.keyValue("Grupos", String.valueOf(api.getGroups().size())));
        ctx.reply(MessageHelper.keyValue("Usuarios", String.valueOf(api.findAllUsers().size())));
    }
}
