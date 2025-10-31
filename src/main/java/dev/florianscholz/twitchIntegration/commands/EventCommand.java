package dev.florianscholz.twitchIntegration.commands;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final TwitchIntegration plugin;

    public EventCommand(TwitchIntegration plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player!");
            return true;
        }

        switch(args[0].toLowerCase()) {
            case "start":
                if(args.length < 2) {
                    sender.sendMessage("Please specify an event name!");
                    return true;
                }
                plugin.getGameEventManager().startEvent(args[1]);
                break;
            case "stop":
                if(plugin.getGameEventManager().isEventRunning()) {
                    plugin.getGameEventManager().stopActiveEvent();
                    sender.sendMessage("Stopped event");
                } else {
                    sender.sendMessage("No event running");
                }
                break;
            case "list":
                sender.sendMessage("Available events:");
                plugin.getGameEventManager().getEvents().forEach((name, event) -> {
                    sender.sendMessage(" - " + name);
                });
                break;
            default:
                sender.sendMessage("Unknown subcommand");
                return false;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if(args.length == 1) {
            completions.add("start");
            completions.add("stop");
            completions.add("list");
        } else if(args.length == 2 && args[0].equalsIgnoreCase("start")) {
            completions.addAll(plugin.getGameEventManager().getEvents().keySet());
        }

        return completions;
    }
}
