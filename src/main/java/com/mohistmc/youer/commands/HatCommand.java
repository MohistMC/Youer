package com.mohistmc.youer.commands;

import com.mohistmc.youer.util.Cooldown;
import com.mohistmc.youer.util.I18n;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2025/11/23 01:37
 */
public class HatCommand extends BukkitCommand {

    public HatCommand(String name) {
        super(name);
        this.description = "Wear the off-hander's belongings on your head";
        this.usageMessage = "/hat";
        this.setPermission("youer.command.hat");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (sender instanceof final Player player) {
            final Cooldown cooldown = new Cooldown(player.getUniqueId(), player.getName(), 10);
            if (!Cooldown.isInCooldown(player.getUniqueId(), player.getName())) {
                if (args.length == 0) {
                    final ItemStack itemStack = player.getInventory().getItemInOffHand().clone();
                    final int amount = itemStack.getAmount();
                    if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() != Material.AIR) {
                        player.sendMessage(I18n.as("hatcmd.already.has.hat"));
                        return true;
                    }
                    if (itemStack != null && itemStack.getType() != Material.AIR && amount == 1) {
                        player.getInventory().setHelmet(itemStack);
                        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                        player.sendMessage(I18n.as("hatcmd.success"));
                        cooldown.start();
                    } else {
                        player.sendMessage(I18n.as("hatcmd.not.holding.item"));
                    }
                    return true;
                }
            } else {
                long timeLeft = Cooldown.getTimeLeft(player.getUniqueId(), player.getName());
                player.sendMessage(I18n.as("hatcmd.cooldown", timeLeft));
            }
            return false;
        }
        sender.sendMessage(I18n.as("hatcmd.not.player"));
        return true;
    }

}