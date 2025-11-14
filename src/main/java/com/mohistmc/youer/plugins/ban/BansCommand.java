package com.mohistmc.youer.plugins.ban;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.EntityAPI;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.plugins.ban.bans.BanItem;
import com.mohistmc.youer.plugins.ban.bans.BanRecipe;
import com.mohistmc.youer.plugins.ban.utils.BanSaveInventory;
import com.mohistmc.youer.plugins.ban.utils.BanUtils;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 5:33:43
 */
public class BansCommand extends Command {

    private final List<String> params = Arrays.asList("add", "show", "setmessage");
    private final List<String> params1 = Arrays.asList("item", "item-moshou", "entity", "enchantment", "recipe", "block");

    public BansCommand(String name) {
        super(name);
        this.description = I18n.as("banscmd.description");
        this.usageMessage = "/bans [add|show|setmessage] [item|item-moshou|entity|enchantment|recipe|block]";
        this.setPermission("youer.command.bans");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }
        String check = I18n.as("banscmd.check");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to perform this command.");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
            return false;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "add" -> {
                if (args.length == 3 && args[1].equals("recipe")) {
                    if (!YouerConfig.ban_recipe_enable) {
                        sender.sendMessage(ChatColor.RED + check);
                        return false;
                    }
                    String name = args[2];
                    if (BanConfig.RECIPE.has(name)) {
                        sender.sendMessage(ChatColor.RED + "This recipe already exists.");
                        return false;
                    }
                    if (!BanRecipe.CACHE.contains(ResourceLocation.parse(name))) {
                        sender.sendMessage(ChatColor.RED + "This recipe does not exist.");
                        return false;
                    }
                    BanRecipe.addBan(player, name);
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + usageMessage);
                    return false;
                }
                switch (args[1]) {
                    case "item" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ITEM, "§4Add bans item");
                        Inventory inventory = banSaveInventory.getInventory();
                        player.openInventory(inventory);
                        BanListener.openInventory = banSaveInventory;
                        return true;
                    }
                    case "item-moshou" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ITEM_MOSHOU, "§4Add bans moshou item");
                        Inventory inventory = banSaveInventory.getInventory();
                        player.openInventory(inventory);
                        BanListener.openInventory = banSaveInventory;
                        return true;
                    }
                    case "entity" -> {
                        if (!YouerConfig.ban_entity_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ENTITY, "§4Add bans entity");
                        Inventory inventory = banSaveInventory.getInventory();
                        player.openInventory(inventory);
                        BanListener.openInventory = banSaveInventory;
                        return true;
                    }
                    case "enchantment" -> {
                        if (!YouerConfig.ban_enchantment_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ENCHANTMENT, "§4Add bans enchantment");
                        Inventory inventory = banSaveInventory.getInventory();
                        player.openInventory(inventory);
                        BanListener.openInventory = banSaveInventory;
                        return true;
                    }
                    case "block" -> {
                        if (!YouerConfig.ban_block_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.BLOCK, "§4Add bans block");
                        Inventory inventory = banSaveInventory.getInventory();
                        player.openInventory(inventory);
                        BanListener.openInventory = banSaveInventory;
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + usageMessage);
                        return false;
                    }
                }
            }
            case "show" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + usageMessage);
                    return false;
                }
                switch (args[1]) {
                    case "item" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.item"));
                        List<String> old = BanConfig.ITEM.getItem();
                        for (String s : BanConfig.ITEM.getItem()) {
                            Material material = Material.matchMaterial(s);
                            if (material != null && !material.isAirSafe()) {
                                wh.addItem(new GUIItem(new ItemStackFactory(material)
                                        .setDisplayName(s)
                                        .addLore("§e" + I18n.as("banscmd.show.lore"))
                                        .build()) {
                                    @Override
                                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                        if (type.isRightClick()) {
                                            old.remove(s);
                                            BanUtils.saveToYaml(u, com.mohistmc.youer.plugins.ban.ClickType.REMOVE, old, BanType.ITEM);
                                            wh.removeItem(this);
                                            wh.openGUI(player);
                                        }
                                    }
                                });
                            }
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "item-moshou" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.item-moshou"));
                        List<String> old = BanConfig.MOSHOU.getMoShouList();
                        for (String s : BanConfig.MOSHOU.getMoShouList()) {
                            Material material = Material.matchMaterial(s);
                            if (material != null && !material.isAirSafe()) {
                                wh.addItem(new GUIItem(new ItemStackFactory(material)
                                        .setDisplayName(s)
                                        .addLore("§e" + I18n.as("banscmd.show.lore"))
                                        .build()) {
                                    @Override
                                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                        if (type.isRightClick()) {
                                            old.remove(s);
                                            BanConfig.MOSHOU.setBaMoShou(old);
                                            wh.removeItem(this);
                                            wh.openGUI(player);
                                        }
                                    }
                                });
                            }
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "entity" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.entity"));
                        List<String> old = BanConfig.ENTITY.getEntity();
                        for (String s : BanConfig.ENTITY.getEntity()) {
                            wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(EntityAPI.getType(s)))
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.plugins.ban.ClickType.REMOVE, old, BanType.ENTITY);
                                        wh.removeItem(this);
                                        wh.openGUI(player);
                                    }
                                }
                            });
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "enchantment" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.enchantment"));
                        List<String> old = BanConfig.ENCHANTMENT.getEnchantment();
                        for (String s : BanConfig.ENCHANTMENT.getEnchantment()) {
                            wh.addItem(new GUIItem(new ItemStackFactory(Material.ENCHANTED_BOOK)
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .setEnchantment(ItemAPI.getEnchantmentByKey(s))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.plugins.ban.ClickType.REMOVE, old, BanType.ENCHANTMENT);
                                        wh.removeItem(this);
                                        wh.openGUI(player);
                                    }
                                }
                            });
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "recipe" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.recipe"));
                        List<String> old = BanConfig.RECIPE.getRecipe();
                        for (String s : BanConfig.RECIPE.getRecipe()) {
                            wh.addItem(new GUIItem(new ItemStackFactory(Material.KNOWLEDGE_BOOK)
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .setEnchantment(ItemAPI.getEnchantmentByKey(s))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.plugins.ban.ClickType.REMOVE, old, BanType.RECIPE);
                                        wh.removeItem(this);
                                        wh.openGUI(player);
                                    }
                                }
                            });
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "block" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.block"));
                        List<String> old = BanConfig.BLOCK.getBlock();
                        for (String s : BanConfig.BLOCK.getBlock()) {
                            Material material = Material.matchMaterial(s);
                            if (material != null && !material.isAirSafe()) {
                                wh.addItem(new GUIItem(new ItemStackFactory(material)
                                        .setDisplayName(s)
                                        .addLore("§e" + I18n.as("banscmd.show.lore"))
                                        .build()) {
                                    @Override
                                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                        if (type.isRightClick()) {
                                            old.remove(s);
                                            BanUtils.saveToYaml(u, com.mohistmc.youer.plugins.ban.ClickType.REMOVE, old, BanType.BLOCK);
                                            wh.removeItem(this);
                                            wh.openGUI(player);
                                        }
                                    }
                                });
                            }
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + usageMessage);
                        return false;
                    }
                }
            }
            case "setmessage" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + usageMessage);
                    return false;
                }
                switch (args[1]) {
                    case "item", "item-moshou" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        ItemStack itemStack = player.getInventory().getItemInMainHand();
                        if (itemStack.isEmpty()) {
                            sender.sendMessage(ChatColor.RED + "Please hold an item in your hand.");
                            return false;
                        }
                        if (BanItem.check(itemStack) || BanItem.checkMoShou(itemStack)) {
                            String result = Arrays.stream(args)
                                    .skip(2)
                                    .collect(Collectors.joining(" "));
                            BanConfig.BAN_MESSAGE.setBanMessage(itemStack.getType().name(), result);
                        } else {
                            sender.sendMessage(ChatColor.RED + "This item is not banned.");
                            return false;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Set the message for " + itemStack.getType().name() + ".");
                        return true;
                    }
                    case "entity" -> {
                        if (!YouerConfig.ban_entity_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        return true;
                    }
                    case "enchantment" -> {
                        if (!YouerConfig.ban_enchantment_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + usageMessage);
                        return false;
                    }
                }
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
                return false;
            }
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && (sender.isOp() || testPermission(sender))) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        }
        if (args.length == 2 && (sender.isOp() || testPermission(sender))) {
            for (String param : params1) {
                if (param.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(param);
                }
            }
        }
        if (args.length == 3 && args[0].equals("add") && args[1].equals("recipe") && (sender.isOp() || testPermission(sender))) {
            return BanRecipe.CACHE.stream()
                    .map(ResourceLocation::toString)
                    .toList();
        }

        return list;
    }
}
