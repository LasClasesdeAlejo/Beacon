package com.colsson.beacon;

import com.colsson.beacon.commands.BeaconCommandRouter;
import com.colsson.beacon.commands.CommandContext;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Adaptador entre Paper (CommandSender) y Beacon (CommandContext).
 *
 * <p>Convierte el CommandSender de Bukkit en un CommandContext
 * que los handlers de Beacon entienden.
 */
public class PaperCommandExecutor implements CommandExecutor, TabCompleter {

    private final BeaconCommandRouter router;
    private final Server server;

    public PaperCommandExecutor(BeaconCommandRouter router, Server server) {
        this.router = router;
        this.server = server;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                              String label, String[] args) {
        CommandContext ctx = new CommandContext(
            sender.getName(),
            args,
            msg -> sender.sendMessage(msg),
            sender instanceof org.bukkit.entity.Player,
            sender instanceof org.bukkit.entity.Player p
                ? p.getUniqueId().toString() : null
        );

        router.dispatch(ctx, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of(
                "help", "info", "reload", "user", "group",
                "permission", "check", "groups", "debug", "history"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "user", "check" -> completions.addAll(getOnlinePlayerNames());
                case "group" -> {
                    completions.addAll(List.of("list", "create"));
                    completions.addAll(getOnlinePlayerNames());
                }
                case "permission" -> completions.addAll(List.of("list", "info", "search"));
                case "debug" -> completions.addAll(List.of("user", "permission"));
                case "groups" -> completions.add("tree");
                case "history" -> completions.addAll(List.of("user", "group", "permission"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "user" -> {
                    switch (args[1].toLowerCase()) {
                        case "group" -> completions.addAll(List.of("add", "remove"));
                        case "permission" -> completions.addAll(List.of("set", "remove", "clear"));
                        default -> completions.addAll(List.of("info", "groups", "permissions", "group", "permission"));
                    }
                }
                case "group" -> {
                    switch (args[1].toLowerCase()) {
                        case "edit" -> completions.addAll(List.of("name", "priority", "description"));
                        case "parent" -> completions.addAll(List.of("set", "remove"));
                        case "permission" -> completions.addAll(List.of("set", "remove", "clear"));
                        default -> completions.addAll(List.of("info", "delete", "edit", "parent", "permission", "parents"));
                    }
                }
                case "permission" -> completions.add("<permiso>");
                case "check" -> completions.add("<permiso>");
                case "debug" -> {
                    switch (args[1].toLowerCase()) {
                        case "user", "permission" -> completions.addAll(getOnlinePlayerNames());
                    }
                }
                case "history" -> completions.add("<targeto>");
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .toList();
    }

    private List<String> getOnlinePlayerNames() {
        return server.getOnlinePlayers().stream()
            .map(Player::getName)
            .toList();
    }
}
