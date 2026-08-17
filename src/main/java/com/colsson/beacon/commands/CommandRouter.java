package com.colsson.beacon.commands;

import com.colsson.beacon.api.BeaconAPI;

import java.util.*;
import java.util.logging.Logger;

/**
 * Enrutador de comandos Paper-independiente.
 * Mapea subcomandos a manejadores con soporte para rutas anidadas.
 *
 * <p>Ejemplo de ruta: "user" → "group" → "add"
 */
public class CommandRouter {

    private final Map<String, CommandHandler> handlers = new LinkedHashMap<>();
    private final Map<String, CommandRouter> subRouters = new LinkedHashMap<>();
    private CommandHandler defaultHandler;
    private final Logger logger;

    public CommandRouter(Logger logger) {
        this.logger = logger;
    }

    public void register(String subcommand, CommandHandler handler) {
        handlers.put(subcommand.toLowerCase(), handler);
    }

    public void registerDefault(CommandHandler handler) {
        this.defaultHandler = handler;
    }

    public CommandRouter createSubRouter(String subcommand) {
        CommandRouter sub = new CommandRouter(logger);
        subRouters.put(subcommand.toLowerCase(), sub);
        return sub;
    }

    /**
     * Despacha un comando con los argumentos dados.
     *
     * @param api       API pública de Beacon
     * @param ctx       contexto del comando
     * @param args      argumentos (el primero es el subcomando)
     * @param prefix    prefijo de ruta ya procesado (para mensajes de ayuda)
     */
    public void dispatch(BeaconAPI api, CommandContext ctx, String[] args, String prefix) {
        if (args.length == 0) {
            if (defaultHandler != null) {
                defaultHandler.execute(api, ctx, args);
            } else {
                showUsage(ctx, prefix);
            }
            return;
        }

        String sub = args[0].toLowerCase();

        // Buscar subrouter anidado
        CommandRouter subRouter = subRouters.get(sub);
        if (subRouter != null) {
            String[] remaining = Arrays.copyOfRange(args, 1, args.length);
            subRouter.dispatch(api, ctx, remaining, prefix + " " + sub);
            return;
        }

        // Buscar handler directo
        CommandHandler handler = handlers.get(sub);
        if (handler != null) {
            String[] remaining = Arrays.copyOfRange(args, 1, args.length);
            handler.execute(api, ctx, remaining);
            return;
        }

        // No encontrado
        showUsage(ctx, prefix);
    }

    /**
     * Despacha sin prefijo (para la raíz).
     */
    public void dispatch(BeaconAPI api, CommandContext ctx, String[] args) {
        dispatch(api, ctx, args, "/beacon");
    }

    private void showUsage(CommandContext ctx, String prefix) {
        ctx.reply("§cUso: " + prefix + " <subcomando>");
        ctx.reply("§7Escribe §f" + prefix + " help §7para ver la ayuda.");
    }

    public Set<String> getSubcommands() {
        return handlers.keySet();
    }

    public Map<String, CommandRouter> getSubRouters() {
        return Collections.unmodifiableMap(subRouters);
    }

    public Logger getLogger() {
        return logger;
    }
}
