package com.mohistmc.youer.feature.entityclear;

import com.mohistmc.youer.api.EntityAPI;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
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

    private static final List<String> TYPES = Arrays.asList("item", "entity", "all", "trash", "add", "show");

    public EntityClearCommand(String name) {
        super(name);
        this.description = I18n.as("entityclear.description");
        this.usageMessage = "/entityclear [item|entity|all|trash|add <item|entity>|show <item|entity>]";
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            // All players can see subcommands (trash is available to everyone)
            for (String type : TYPES) {
                if (type.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(type);
                }
            }
        } else if (!sender.isOp() && !testPermission(sender)) {
            return list;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("show"))) {
            for (String type : Arrays.asList("item", "entity")) {
                if (type.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(type);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("entity")) {
            // Individual entity suggestions
            for (var key : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
                String name = key.toString();
                if (name.toLowerCase().startsWith(args[2].toLowerCase())) {
                    list.add(name);
                }
                String blacklistName = "!" + name;
                if (blacklistName.toLowerCase().startsWith(args[2].toLowerCase())) {
                    list.add(blacklistName);
                }
            }
            // Mod wildcard suggestions (modid:*)
            java.util.Set<String> namespaces = new java.util.HashSet<>();
            for (var key : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
                namespaces.add(key.getNamespace());
            }
            for (String ns : namespaces) {
                String wildcard = ns + ":*";
                if (wildcard.toLowerCase().startsWith(args[2].toLowerCase())) {
                    list.add(wildcard);
                }
                String blacklistWildcard = "!" + ns + ":*";
                if (blacklistWildcard.toLowerCase().startsWith(args[2].toLowerCase())) {
                    list.add(blacklistWildcard);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("item")) {
            // Individual item suggestions
            for (var key : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                String name = key.toString();
                if (name.toLowerCase().startsWith(args[2].toLowerCase())) {
                    list.add(name);
                }
            }
            // Mod wildcard suggestions (modid:*)
            java.util.Set<String> namespaces = new java.util.HashSet<>();
            for (var key : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                namespaces.add(key.getNamespace());
            }
            for (String ns : namespaces) {
                String wildcard = ns + ":*";
                if (wildcard.toLowerCase().startsWith(args[2].toLowerCase())) {
                    list.add(wildcard);
                }
            }
        }
        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        // Trash subcommand is available to all players without permission check
        if (args.length > 0 && args[0].equalsIgnoreCase("trash")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + I18n.as("entityclear.error.notplayer"));
                return false;
            }
            EntityClearTrash.openTrash(player);
            return true;
        }

        if (!sender.hasPermission("youer.command.entityclear")) {
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
            case "entity" -> {
                EntityClear.run_monster();
                sender.sendMessage(I18n.as("entityclear.entity.started"));
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
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("entityclear.usage"));
                    return false;
                }
                switch (args[1].toLowerCase(Locale.ENGLISH)) {
                    case "item" -> {
                        if (args.length >= 3) {
                            String itemName = args[2];
                            boolean isWildcard = itemName.matches("^[a-z0-9_.-]+:\\*$");
                            if (!isWildcard) {
                                net.minecraft.resources.ResourceLocation itemKey = net.minecraft.resources.ResourceLocation.parse(itemName);
                                if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(itemKey)) {
                                    sender.sendMessage(ChatColor.RED + I18n.as("entityclear.add.item.invalid").formatted(itemName));
                                    return false;
                                }
                            }

                            List<String> old = new ArrayList<>(EntityClear.getWhitelist(true));
                            if (old.contains(itemName)) {
                                sender.sendMessage(ChatColor.YELLOW + I18n.as("entityclear.add.item.exists").formatted(itemName));
                                return false;
                            }

                            old.add(itemName);
                            EntityClear.saveItemWhitelist(old);
                            sender.sendMessage(ChatColor.GREEN + I18n.as("entityclear.add.item.success").formatted(itemName));
                            return true;
                        }

                        EntityClearInventory clearInventory = new EntityClearInventory(EntityClearType.ITEM, I18n.as("entityclear.gui.add.item"));
                        Inventory inventory = clearInventory.getInventory();
                        player.openInventory(inventory);
                        EntityClearListener.openInventory = clearInventory;
                        return true;
                    }
                    case "entity" -> {
                        if (args.length >= 3) {
                            String entityName = args[2];
                            String rawName = entityName.startsWith("!") ? entityName.substring(1) : entityName;
                            boolean isWildcard = rawName.matches("^[a-z0-9_.-]+:\\*$");
                            if (!isWildcard) {
                                net.minecraft.resources.ResourceLocation entityKey = net.minecraft.resources.ResourceLocation.parse(rawName);
                                if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(entityKey)) {
                                    sender.sendMessage(ChatColor.RED + I18n.as("entityclear.add.entity.invalid").formatted(entityName));
                                    return false;
                                }
                            }

                            List<String> old = new ArrayList<>(EntityClear.getWhitelist(false));
                            if (old.contains(entityName)) {
                                if (entityName.startsWith("!")) {
                                    sender.sendMessage(ChatColor.YELLOW + I18n.as("entityclear.add.entity.blacklist_exists").formatted(entityName));
                                } else {
                                    sender.sendMessage(ChatColor.YELLOW + I18n.as("entityclear.add.entity.exists").formatted(entityName));
                                }
                                return false;
                            }

                            old.add(entityName);
                            EntityClear.saveMonsterWhitelist(old);
                            if (entityName.startsWith("!")) {
                                sender.sendMessage(ChatColor.GREEN + I18n.as("entityclear.add.entity.blacklist_success").formatted(entityName));
                            } else {
                                sender.sendMessage(ChatColor.GREEN + I18n.as("entityclear.add.entity.success").formatted(entityName));
                            }
                            return true;
                        }

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
                            Material material = s.endsWith(":*") ? Material.BARRIER : Material.matchMaterial(s);
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
                            Material icon;
                            String displayName;
                            if (s.startsWith("!")) {
                                // Blacklist entry - force clear
                                icon = Material.REDSTONE_BLOCK;
                                displayName = "§c" + s;
                            } else if (s.endsWith(":*")) {
                                // Wildcard whitelist
                                icon = Material.BARRIER;
                                displayName = s;
                            } else {
                                icon = ItemAPI.getEggMaterial(EntityAPI.getType(s));
                                displayName = s;
                            }
                            wh.addItem(new GUIItem(new ItemStackFactory(icon)
                                    .setDisplayName(displayName)
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
                        // Show blacklist summary in chat
                        List<String> blacklist = old.stream().filter(e -> e.startsWith("!")).toList();
                        if (!blacklist.isEmpty()) {
                            player.sendMessage(ChatColor.RED + I18n.as("entityclear.show.blacklist") + " §f" + String.join("§7, §f", blacklist));
                        }
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
