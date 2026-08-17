package com.colsson.beacon.commands;

import java.util.function.Consumer;

/**
 * Contexto de ejecución de un comando.
 * Paper-independiente: el adaptador Paper crea esto desde CommandSender.
 */
public class CommandContext {

    private final String senderName;
    private final String[] args;
    private final Consumer<String> responder;
    private final boolean isPlayer;
    private final String playerUuid;

    public CommandContext(String senderName, String[] args,
                          Consumer<String> responder, boolean isPlayer, String playerUuid) {
        this.senderName = senderName;
        this.args = args;
        this.responder = responder;
        this.isPlayer = isPlayer;
        this.playerUuid = playerUuid;
    }

    public String senderName() { return senderName; }
    public String[] args() { return args; }
    public boolean isPlayer() { return isPlayer; }
    public String playerUuid() { return playerUuid; }

    public void reply(String message) {
        responder.accept(message);
    }

    public void reply(String... lines) {
        for (String line : lines) {
            responder.accept(line);
        }
    }
}
