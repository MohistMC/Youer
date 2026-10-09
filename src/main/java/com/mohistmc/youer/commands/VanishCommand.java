package com.mohistmc.youer.commands;

import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

/**
 * @author Mgazul
 * @date 2025/11/23 01:45
 */
public class VanishCommand extends BukkitCommand {

    public static ArrayList<Player> vanished = new ArrayList<>();

    public VanishCommand(String name) {
        super(name);
        this.description = "Invisibility yourself";
        this.usageMessage = "/vanish";
        this.setPermission("youer.command.vanish");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(I18n.as("vanish.console"));
            return false;
        }
        if (args.length != 0 || !p.isOp()) {
            if (args.length == 1 && p.isOp()) {
                if (Bukkit.getServer().getPlayer(args[0]) != null) {
                    Player p2 = Bukkit.getServer().getPlayer(args[0]);
                    if (!VanishCommand.vanished.contains(p2)) {
                        for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                            pl.hidePlayer(p2);
                        }
                        VanishCommand.vanished.add(p2);
                        p2.sendMessage(I18n.as("vanish.on"));
                        return true;
                    }
                    for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                        pl.showPlayer(p2);
                    }
                    VanishCommand.vanished.remove(p2);
                    p2.sendMessage(I18n.as("vanish.off"));
                    return true;
                }
                else {
                    p.sendMessage(I18n.as("vanish.notonline"));
                }
            }
            return false;
        }
        if (!VanishCommand.vanished.contains(p)) {
            for (Player pl2 : Bukkit.getServer().getOnlinePlayers()) {
                pl2.hidePlayer(p);
            }
            VanishCommand.vanished.add(p);
            p.sendMessage(I18n.as("vanish.on"));
            return true;
        }
        for (Player pl2 : Bukkit.getServer().getOnlinePlayers()) {
            pl2.showPlayer(p);
        }
        VanishCommand.vanished.remove(p);
        p.sendMessage(I18n.as("vanish.off"));
        return true;
    }
}
