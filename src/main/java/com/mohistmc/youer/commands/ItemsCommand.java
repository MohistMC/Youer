/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2025.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.youer.commands;

import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.plugins.item.ItemsConfig;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemsCommand extends Command {

    private final List<String> params = Arrays.asList("name", "save", "remove", "list", "get", "modeldata", "lore", "attribute", "unattribute", "rarity");

    public ItemsCommand(String name) {
        super(name);
        this.description = I18n.as("itemscmd.description");
        this.usageMessage = "/items [name|save|list|get|remove|lore|attribute|unattribute|rarity|modeldata]";
        this.setPermission("youer.command.items");
    }

    public static void info(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        PlayerAPI.sendMessageByCopy(player, ChatColor.GRAY + I18n.as("items.info.type") + " ", itemStack.getType().name());
        PlayerAPI.sendMessageByCopy(player, ChatColor.GRAY + I18n.as("items.info.key") + " ", itemStack.getType().getKey().asString());
        PlayerAPI.sendMessageByCopy(player, ChatColor.GRAY + I18n.as("items.info.name") + " ", nmsItem.getHoverName().getString());
        player.sendMessage(ChatColor.GRAY + I18n.as("items.info.moditem") + " %s".formatted(itemStack.getType().isModItem));
        player.sendMessage(ChatColor.GRAY + I18n.as("items.info.modblock") + " %s".formatted(itemStack.getType().isModBlock));
        if (itemStack.hasCustomModelData()) {
            player.sendMessage(ChatColor.GRAY + I18n.as("items.info.custommodeldata") + " %s".formatted(itemStack.getCustomModelData()));
        }
        player.sendMessage(ChatColor.GRAY + I18n.as("items.info.itemflags") + " %s".formatted(itemStack.getItemFlags()));

        player.sendMessage(ChatColor.GRAY + I18n.as("items.info.amount") + " %s".formatted(itemStack.getAmount()));
        player.sendMessage(ChatColor.GRAY + I18n.as("items.info.maxstacksize") + " %s".formatted(itemStack.getMaxStackSize()));

        if (itemStack.hasItemMeta()) {
            player.sendMessage(ChatColor.GRAY + I18n.as("items.info.hasitemmeta") + " %s".formatted(true));

            if (itemStack.getItemMeta().hasDisplayName()) {
                PlayerAPI.sendMessageByCopy(player, ChatColor.GRAY + I18n.as("items.info.displayname") + " ", itemStack.getItemMeta().getDisplayName());
            }

            if (itemStack.getItemMeta().hasLore()) {
                player.sendMessage(ChatColor.GRAY + I18n.as("items.info.lore") + ":");
                itemStack.getItemMeta().getLore().forEach(lore -> player.sendMessage("  " + lore));
            }

            if (itemStack.getItemMeta().hasEnchants()) {
                player.sendMessage(ChatColor.GRAY + I18n.as("items.info.enchants") + ":");
                itemStack.getItemMeta().getEnchants().forEach((enchant, level) ->
                        player.sendMessage("  " + enchant.getKey().toString() + " - " + level));
            }
        } else {
            player.sendMessage(ChatColor.GRAY + I18n.as("items.info.hasitemmeta") + " %s".formatted(false));
        }

        if (itemStack.getType().getMaxDurability() > 0) {
            player.sendMessage(ChatColor.GRAY + I18n.as("items.info.durability") + " %s/%s".formatted(
                    itemStack.getType().getMaxDurability() - itemStack.getDurability(),
                    itemStack.getType().getMaxDurability()));
        }
        PlayerAPI.sendMessageByCopy(player, ChatColor.GRAY  + "Base64 - ", ItemAPI.getBase64byBukkit(itemStack));
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (!sender.isOp() && !testPermission(sender)) {
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        } else if (args.length == 2) {
            switch (args[0]) {
                case "get", "remove" -> {
                    return ItemsConfig.INSTANCE.getItemStrings();
                }
                case "rarity" -> {
                    return Arrays.stream(ItemRarity.values()).map(rarity -> rarity.name().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
                }
                case "lore" -> {
                    return Arrays.asList("add", "set", "remove");
                }
                case "attribute", "unattribute" -> {
                    String partialAttribute = args[1].toLowerCase();
                    for (org.bukkit.attribute.Attribute attr : org.bukkit.attribute.Attribute.values()) {
                        String name = attr.name().toLowerCase();
                        if (name.startsWith(partialAttribute)) {
                            list.add(name.replace("generic_", ""));
                        }
                    }
                }
            }
        } else if (args.length == 3 && args[0].equals("attribute")) {
            list.addAll(Arrays.asList("1.0", "2.0", "5.0", "10.0", "-1.0"));
        } else if (args.length == 4 && args[0].equals("attribute")) {
            String partialSlot = args[3].toLowerCase();
            List<String> slots = new ArrayList<>();
            slots.add("ANY");
            for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                slots.add(slot.name());
            }
            for (String slot : slots) {
                if (slot.toLowerCase().startsWith(partialSlot)) {
                    list.add(slot);
                }
            }
        } else if (args.length == 3 && args[0].equals("lore") &&
                (args[1].equals("set") || args[1].equals("remove")) &&
                sender instanceof Player player) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
                List<String> loreLines = itemStack.getItemMeta().getLore();
                if (loreLines != null) {
                    for (int i = 0; i < loreLines.size(); i++) {
                        list.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 4 && args[0].equals("lore") &&
                args[1].equals("set") && sender instanceof Player player) {
            try {
                int line = Integer.parseInt(args[2]);
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
                    List<String> loreLines = itemStack.getItemMeta().getLore();
                    if (loreLines != null && line >= 0 && line < loreLines.size()) {
                        list.add(loreLines.get(line).replace(ChatColor.COLOR_CHAR, '&'));
                    }
                }
            } catch (NumberFormatException ignored) {
                // Ignore invalid line numbers
            }
        }

        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + I18n.as("error.notplayer"));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", usageMessage));
            return false;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "name" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items name <string>"));
                    return false;
                }
                ItemAPI.name(player.getInventory().getItemInMainHand(), args[1]);
                sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.completeSet"));
                return true;
            }
            case "rarity" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items rarity <rarity>"));
                    return false;
                }
                ItemAPI.rarity(player.getInventory().getItemInMainHand(), ItemRarity.byName(args[1]));
                sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.completeRarity"));
                return true;
            }
            case "save" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items save <name>"));
                    return false;
                }
                ItemsConfig.INSTANCE.put("items." + args[1], itemStack);
                sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.itemSavedSuccessfully"));
                return true;
            }
            case "lore" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }

                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items lore <add|set|remove> [line] [text...]"));
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "add" -> {
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items lore add [line] <text...>"));
                            return false;
                        }

                        List<String> currentLore = itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()
                                ? new ArrayList<>(itemStack.getItemMeta().getLore())
                                : new ArrayList<>();

                        // Check if the second parameter is a number (line number)
                        int insertLine = -1;
                        String loreText;
                        try {
                            insertLine = Integer.parseInt(args[2]);
                            // If it's a number, then text starts from the 4th parameter
                            if (args.length < 4) {
                                sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items lore add [line] <text...>"));
                                return false;
                            }
                            loreText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        } catch (NumberFormatException e) {
                            // If not a number, then text starts from the 3rd parameter
                            loreText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                        }

                        // Support adding multiple empty lines through newline characters
                        String[] lines = loreText.split("\\\\n");

                        if (insertLine >= 0) {
                            // Insert after the specified line
                            if (insertLine > currentLore.size()) {
                                sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.lineNumberOutOfRange"));
                                return false;
                            }
                            // Insert all lines at the specified position
                            for (int i = lines.length - 1; i >= 0; i--) {
                                currentLore.add(insertLine, lines[i]);
                            }
                        } else {
                            // Add all lines at the end
                            Collections.addAll(currentLore, lines);
                        }

                        ItemAPI.lore(itemStack, currentLore);
                        sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.completeLore"));
                        return true;
                    }

                    case "set" -> {
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items lore set <line> <text...>"));
                            return false;
                        }

                        try {
                            int line = Integer.parseInt(args[2]);
                            if (line < 0) {
                                sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.lineNumberMustBePositive"));
                                return false;
                            }

                            String loreText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                            List<String> currentLore = itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()
                                    ? new ArrayList<>(itemStack.getItemMeta().getLore())
                                    : new ArrayList<>();

                            // If the specified line exceeds the current lore length, extend the list
                            while (currentLore.size() <= line) {
                                currentLore.add("");
                            }

                            currentLore.set(line, loreText);
                            ItemAPI.lore(itemStack, currentLore);
                            sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.completeLore"));
                            return true;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.invalidLineNumber", args[2]));
                            return false;
                        }
                    }

                    case "remove" -> {
                        if (args.length != 3) {
                            sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items lore remove <line>"));
                            return false;
                        }

                        try {
                            int line = Integer.parseInt(args[2]);
                            if (line < 0) {
                                sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.lineNumberMustBePositive"));
                                return false;
                            }

                            if (!itemStack.hasItemMeta() || !itemStack.getItemMeta().hasLore()) {
                                sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.itemHasNoLore"));
                                return false;
                            }

                            List<String> currentLore = new ArrayList<>(itemStack.getItemMeta().getLore());
                            if (line >= currentLore.size()) {
                                sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.lineNumberOutOfRange"));
                                return false;
                            }

                            currentLore.remove(line);
                            ItemAPI.lore(itemStack, currentLore);
                            sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.completeLore"));
                            return true;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.invalidLineNumber", args[2]));
                            return false;
                        }
                    }

                    default -> {
                        sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items lore <add|set|remove> [line] [text...]"));
                        return false;
                    }
                }
            }
            case "modeldata" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items modeldata <number>"));
                    return false;
                }
                if (!args[1].matches("[0-9]+")) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items modeldata <number>"));
                    return false;
                }
                ItemAPI.customModelData(itemStack, Integer.parseInt(args[1]));
                sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.modelDataSetSuccess", args[1]));
                return true;
            }
            case "get" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items get <name>"));
                    return false;
                }
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(ItemsConfig.INSTANCE.get(args[1]));
                } else {
                    sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.inventoryFull"));
                    return false;
                }
                return true;
            }
            case "remove" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items remove <name>"));
                    return false;
                }
                ItemsConfig.INSTANCE.remove(args[1]);
                sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.removedItemp1") + args[1] + ChatColor.GREEN + I18n.as("itemscmd.removedItemp2"));
                return true;
            }
            case "list" -> {
                DemoGUI wh = new DemoGUI(I18n.as("itemscmd.customizeItemsGUI"));
                for (ItemStack s : ItemsConfig.INSTANCE.getItems()) {
                    wh.addItem(new GUIItem(s) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack1) {
                            if (player.getInventory().firstEmpty() != -1) {
                                player.getInventory().addItem(itemStack1);
                            }
                        }
                    });
                }
                wh.openGUI(player);
                return true;
            }
            case "attribute" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }

                if (args.length < 3 || args.length > 4) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items attribute <attribute> <value> [slot]"));
                    return false;
                }

                String attributeName = args[1].toUpperCase();
                org.bukkit.attribute.Attribute attribute = getAttributeFromString(attributeName);
                if (attribute == null) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.invalidAttribute"));
                    return false;
                }

                double value;
                try {
                    value = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.invalidValue"));
                    return false;
                }

                org.bukkit.inventory.EquipmentSlot slot = org.bukkit.inventory.EquipmentSlot.HAND;
                String slotName = "HAND";
                if (args.length == 4) {
                    String slotInput = args[3].toUpperCase();
                    if (slotInput.equals("ANY") || slotInput.equals("ALL")) {
                        slot = null;
                        slotName = "ANY";
                    } else {
                        try {
                            slot = org.bukkit.inventory.EquipmentSlot.valueOf(slotInput);
                            slotName = slot.name();
                        } catch (IllegalArgumentException e2) {
                            sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.invalidSlot"));
                            return false;
                        }
                    }
                }

                ItemAPI.attribute(itemStack, attribute, value, slot);
                sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.attributeSuccess", attribute.name(), String.valueOf(value), slotName));
                return true;
            }
            case "unattribute" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }

                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", "/items unattribute <attribute>"));
                    return false;
                }

                String attributeName = args[1].toUpperCase();
                org.bukkit.attribute.Attribute attribute = getAttributeFromString(attributeName);
                if (attribute == null) {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.invalidAttribute"));
                    return false;
                }

                boolean removed = ItemAPI.removeAttribute(itemStack, attribute);
                if (removed) {
                    sender.sendMessage(ChatColor.GREEN + I18n.as("itemscmd.attributeRemoveSuccess", attribute.name()));
                } else {
                    sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.attributeNotFound"));
                }
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + I18n.as("itemscmd.usage", usageMessage));
                return false;
            }
        }
    }

    private org.bukkit.attribute.Attribute getAttributeFromString(String input) {
        try {
            return org.bukkit.attribute.Attribute.valueOf(input);
        } catch (IllegalArgumentException ex) {
            if (!input.startsWith("GENERIC_")) {
                try {
                    return org.bukkit.attribute.Attribute.valueOf("GENERIC_" + input);
                } catch (IllegalArgumentException ex2) {
                    // Continue to next check
                }
            }
            String normalized = input.replace("GENERIC_", "").replace(" ", "_").toUpperCase();
            for (org.bukkit.attribute.Attribute attr : org.bukkit.attribute.Attribute.values()) {
                String attrName = attr.name().replace("GENERIC_", "");
                if (attrName.equals(normalized)) {
                    return attr;
                }
            }
            return null;
        }
    }
}
