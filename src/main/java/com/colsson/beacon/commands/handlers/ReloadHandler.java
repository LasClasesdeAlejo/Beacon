package com.colsson.beacon.commands.handlers;

import com.colsson.beacon.commands.*;
import com.colsson.beacon.api.BeaconAPI;

/**
 * /beacon reload — Recarga la caché desde MySQL.
 */
public class ReloadHandler implements CommandHandler {

    @Override
    public void execute(BeaconAPI api, CommandContext ctx, String[] args) {
        api.reload();
        ctx.reply(MessageHelper.success("Beacon recargado correctamente."));
    }
}
