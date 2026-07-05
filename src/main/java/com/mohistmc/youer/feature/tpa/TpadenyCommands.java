package com.mohistmc.youer.feature.tpa;

import com.mohistmc.youer.util.I18n;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpadenyCommands extends Command {

    public TpadenyCommands(String name) {
        super(name);
        this.usageMessage = "/tpadeny";
        this.setPermission("youer.command.tpa");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (TpaCommands.tpa.containsKey(player)) {
                final Player a = TpaCommands.tpa.get(player);
                a.sendMessage(I18n.as("tpadenycommands.successfully.you"));
                player.sendMessage(I18n.as("tpadenycommands.successfully.me"));
                TpaCommands.tpa.remove(player);
            } else {
                sender.sendMessage(I18n.as("tpadenycommands.nokey"));
            }
        }
        return false;
    }
}
