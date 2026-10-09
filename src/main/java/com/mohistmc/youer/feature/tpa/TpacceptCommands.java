package com.mohistmc.youer.feature.tpa;

import com.mohistmc.youer.util.I18n;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpacceptCommands extends Command {

    public TpacceptCommands(String name) {
        super(name);
        this.usageMessage = "/tpaccept";
        this.setPermission("youer.command.tpaccept");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player player) {
            if (TpaCommands.tpa.containsKey(player)) {
                final Player a = TpaCommands.tpa.get(player);
                a.teleport(player);
                player.sendMessage(I18n.as("tpacceptcommands.successfully.me"));
                a.sendMessage(I18n.as("tpacceptcommands.successfully.you"));
                TpaCommands.tpa.remove(player);
            } else {
                sender.sendMessage(I18n.as("tpacceptcommands.nokey"));
            }
        }
        return false;
    }
}
