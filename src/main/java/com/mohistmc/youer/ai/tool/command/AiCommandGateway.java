package com.mohistmc.youer.ai.tool.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface AiCommandGateway {
    List<AiCommandDescriptor> search(CommandSender sender, String query, int limit);
    boolean dispatchPlayer(Player player, String command);
    boolean dispatchConsole(String command);
}
