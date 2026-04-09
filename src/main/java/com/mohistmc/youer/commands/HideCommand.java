
package com.mohistmc.youer.commands;

import com.mohistmc.youer.util.I18n;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HideCommand extends BukkitCommand {

    public static boolean hide = false;

    public HideCommand(String name) {
        super(name);
        this.description = I18n.as("hidecmd.description");
        this.usageMessage = "/hide";
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;
        if (sender instanceof Player player) {
            if (args.length == 0) {
                if (!hide) {
                    for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                        if (player.canSee(onlinePlayer)) {
                            player.hidePlayer(onlinePlayer);
                        }
                    }
                    hide = true;
                    sender.sendMessage(I18n.as("hidecmd.hidden"));
                } else {
                    for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                        if (!player.canSee(onlinePlayer)) {
                            player.showPlayer(onlinePlayer);
                        }
                    }
                    hide = false;
                    sender.sendMessage(I18n.as("hidecmd.shown"));
                }
                return true;
            }
        }
        return false;
    }
}