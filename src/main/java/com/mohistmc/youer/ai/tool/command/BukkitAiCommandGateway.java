package com.mohistmc.youer.ai.tool.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BukkitAiCommandGateway implements AiCommandGateway {
    @Override
    public List<AiCommandDescriptor> search(CommandSender sender, String query, int limit) {
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        Set<Command> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<AiCommandDescriptor> result = new ArrayList<>();
        Bukkit.getCommandMap().getKnownCommands().forEach((key, command) -> {
            if (result.size() >= Math.min(25, limit) || !seen.add(command)
                    || !command.testPermissionSilent(sender)) return;
            String text = String.join(" ", key, command.getName(), String.join(" ", command.getAliases()),
                    command.getDescription(), command.getUsage()).toLowerCase(Locale.ROOT);
            if (!text.contains(needle)) return;
            int separator = key.indexOf(':');
            result.add(new AiCommandDescriptor(command.getName(),
                    separator < 0 ? "" : key.substring(0, separator), command.getAliases(),
                    command.getDescription(), command.getUsage()));
        });
        return List.copyOf(result);
    }

    @Override public boolean dispatchPlayer(Player player, String command) {
        return Bukkit.dispatchCommand(player, command);
    }
    @Override public boolean dispatchConsole(String command) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
