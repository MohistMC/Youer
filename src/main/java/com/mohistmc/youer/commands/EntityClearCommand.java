package com.mohistmc.youer.commands;

import com.mohistmc.youer.api.EntityAPI;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.feature.EntityClear;
import com.mohistmc.youer.feature.EntityClearInventory;
import com.mohistmc.youer.feature.EntityClearListener;
import com.mohistmc.youer.feature.EntityClearType;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2026/8/6
 */
public class EntityClearCommand extends BukkitCommand {

    private static final List<String> TYPES = Arrays.asList("item", "monster", "all", "add", "show");

    public EntityClearCommand(String name) {
        super(name);
        this.description = I18n.as("entityclear.description");
        this.usageMessage = "/entityclear [item|monster|all|add <item|entity>|show <item|entity>]";
        this.setPermission("youer.command.entityclear");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.isOp() && !testPermission(sender)) {
            return list;
        }
        if (args.length == 1) {
            for (String type : TYPES) {
                if (type.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(type);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("show"))) {
            for (String type : Arrays.asList("item", "entity")) {
                if (type.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(type);
                }
            }
        }
        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            EntityClear.run_item();
            EntityClear.run_monster();
            sender.sendMessage(I18n.as("entityclear.all.started"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "item" -> {
                EntityClear.run_item();
                sender.sendMessage(I18n.as("entityclear.item.started"));
            }
            case "monster" -> {
                EntityClear.run_monster();
                sender.sendMessage(I18n.as("entityclear.monster.started"));
            }
            case "all" -> {
                EntityClear.run_item();
                EntityClear.run_monster();
                sender.sendMessage(I18n.as("entityclear.all.started"));
            }
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + I18n.as("entityclear.error.notplayer"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("entityclear.usage"));
                    return false;
                }
                switch (args[1].toLowerCase(Locale.ENGLISH)) {
                    case "item" -> {
                        EntityClearInventory clearInventory = new EntityClearInventory(EntityClearType.ITEM, I18n.as("entityclear.gui.add.item"));
                        Inventory inventory = clearInventory.getInventory();
                        player.openInventory(inventory);
                        EntityClearListener.openInventory = clearInventory;
                        return true;
                    }
                    case "entity" -> {
                        EntityClearInventory clearInventory = new EntityClearInventory(EntityClearType.ENTITY, I18n.as("entityclear.gui.add.entity"));
                        Inventory inventory = clearInventory.getInventory();
                        player.openInventory(inventory);
                        EntityClearListener.openInventory = clearInventory;
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + I18n.as("entityclear.usage"));
                        return false;
                    }
                }
            }
            case "show" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + I18n.as("entityclear.error.notplayer"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("entityclear.usage"));
                    return false;
                }
                switch (args[1].toLowerCase(Locale.ENGLISH)) {
                    case "item" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("entityclear.show.item"));
                        List<String> old = new ArrayList<>(EntityClear.getWhitelist(true));
                        for (String s : old) {
                            Material material = Material.matchMaterial(s);
                            if (material != null && !material.isAirSafe()) {
                                wh.addItem(new GUIItem(new ItemStackFactory(material)
                                        .setDisplayName(s)
                                        .addLore("§e" + I18n.as("entityclear.show.lore"))
                                        .build()) {
                                    @Override
                                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                        if (type.isRightClick()) {
                                            old.remove(s);
                                            EntityClear.saveItemWhitelist(old);
                                            wh.removeItem(this);
                                            wh.openGUI(player);
                                            u.sendMessage(ChatColor.GREEN + I18n.as("entityclear.remove.success"));
                                        }
                                    }
                                });
                            }
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "entity" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("entityclear.show.entity"));
                        List<String> old = new ArrayList<>(EntityClear.getWhitelist(false));
                        for (String s : old) {
                            wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(EntityAPI.getType(s)))
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("entityclear.show.lore"))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        EntityClear.saveMonsterWhitelist(old);
                                        wh.removeItem(this);
                                        wh.openGUI(player);
                                        u.sendMessage(ChatColor.GREEN + I18n.as("entityclear.remove.success"));
                                    }
                                }
                            });
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + I18n.as("entityclear.usage"));
                        return false;
                    }
                }
            }
            default -> {
                sender.sendMessage(ChatColor.RED + I18n.as("entityclear.usage"));
                return false;
            }
        }
        return true;
    }
}
