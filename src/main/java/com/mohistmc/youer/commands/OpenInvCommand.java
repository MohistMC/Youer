package com.mohistmc.youer.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2025/11/23 02:14
 */
public class OpenInvCommand extends BukkitCommand {

    public OpenInvCommand(String name) {
        super(name);
        this.description = "Check out the Player Inventory/Ender chest";
        this.usageMessage = "/openinv";
        this.setPermission("youer.command.openinv");
    }

    private final List<String> params = Arrays.asList("enderchest", "inventory");


    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if ((sender.isOp() || testPermission(sender))) {
            if (args.length == 1) {
                for (String param : params) {
                    if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                        list.add(param);
                    }
                }
            } else if (args.length == 2) {
                if (params.stream().anyMatch(p -> p.equalsIgnoreCase(args[0]))) {
                    for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                        String playerName = offlinePlayer.getName();
                        if (playerName != null && playerName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            list.add(playerName);
                        }
                    }
                    return list;
                }
            }
        }

        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (sender instanceof final Player player) {
            if (args.length == 2 && args[0].equalsIgnoreCase("enderchest") && player.isOp() && Bukkit.getServer().getPlayer(args[1]) != null) {
                final Player tPlayer = Bukkit.getServer().getPlayer(args[1]);
                player.openInventory(tPlayer.getEnderChest());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("inventory") && player.isOp() && Bukkit.getServer().getPlayer(args[1]) != null) {
                final Player tPlayer = Bukkit.getServer().getPlayer(args[1]);
                player.openInventory(tPlayer.getInventory());
            }
            return true;
        }
        sender.sendMessage("§c?");
        return true;
    }
}
