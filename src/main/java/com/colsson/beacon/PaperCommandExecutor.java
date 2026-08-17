package com.colsson.beacon;

import com.colsson.beacon.api.BeaconAPI;
import com.colsson.beacon.commands.BeaconCommandRouter;
import com.colsson.beacon.commands.CommandContext;
import com.colsson.beacon.commands.TabCompletionEngine;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Adaptador entre Paper (CommandSender) y Beacon (CommandContext).
 *
 * <p>Tab completion delega a {@link TabCompletionEngine} (testable sin Paper).
 */
public class PaperCommandExecutor implements CommandExecutor, TabCompleter {

    private final BeaconCommandRouter router;
    private final TabCompletionEngine tabEngine;

    public PaperCommandExecutor(BeaconCommandRouter router, Server server, BeaconAPI api) {
        this.router = router;
        this.tabEngine = new TabCompletionEngine(
            () -> server.getOnlinePlayers().stream().map(Player::getName).toList(),
            () -> api.getGroups().stream().map(g -> g.name()).toList()
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                              String label, String[] args) {
        CommandContext ctx = new CommandContext(
            sender.getName(),
            args,
            msg -> sender.sendMessage(msg),
            sender instanceof Player,
            sender instanceof Player p ? p.getUniqueId().toString() : null
        );
        router.dispatch(ctx, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String label, String[] args) {
        return tabEngine.complete(args);
    }
}
