package com.colsson.beacon.commands;

import com.colsson.beacon.api.BeaconAPI;

/**
 * Interfaz para manejadores de comandos.
 * Paper-independiente.
 */
@FunctionalInterface
public interface CommandHandler {

    /**
     * Ejecuta un subcomando.
     *
     * @param api   API pública de Beacon
     * @param ctx   contexto del comando (sender, args, respuesta)
     * @param args  argumentos restantes después del subcomando procesado
     */
    void execute(BeaconAPI api, CommandContext ctx, String[] args);
}
