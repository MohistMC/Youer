package com.mohistmc.youer.feature.ban;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.EntityAPI;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.feature.ban.bans.BanItem;
import com.mohistmc.youer.feature.ban.bans.BanRecipe;
import com.mohistmc.youer.feature.ban.bans.BanStructure;
import com.mohistmc.youer.feature.ban.bans.BanWorld;
import com.mohistmc.youer.feature.ban.utils.BanSaveInventory;
import com.mohistmc.youer.feature.ban.utils.BanUtils;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
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

    private final List<String> params = Arrays.asList("add", "show", "setmessage", "reload");
    private final List<String> params1 = Arrays.asList("item", "item-moshou", "entity", "enchantment", "recipe", "block", "nbt", "world", "structure");

    public BansCommand(String name) {
        super(name);
        this.description = I18n.as("banscmd.description");
        this.usageMessage = "/bans [add|show|setmessage|reload] [item|item-moshou|entity|enchantment|recipe|block|world|structure]";
        this.setPermission("youer.command.bans");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }
        String check = I18n.as("banscmd.check");

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            BanConfig.reloadAll();
            sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.reload.success"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.error.notplayer"));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
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
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.recipe.exists"));
                        return false;
                    }
                    if (!BanRecipe.CACHE.contains(ResourceLocation.parse(name))) {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.recipe.notexists"));
                        return false;
                    }
                    BanRecipe.addBan(player, name);
                    return true;
                }
                if (args.length == 3 && args[1].equals("world")) {
                    if (!YouerConfig.ban_world_enable) {
                        sender.sendMessage(ChatColor.RED + check);
                        return false;
                    }
                    String name = args[2];
                    if (BanConfig.WORLD.has(name)) {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.world.exists"));
                        return false;
                    }
                    if (!BanWorld.CACHE.contains(ResourceLocation.parse(name))) {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.world.notexists"));
                        return false;
                    }
                    BanWorld.addBan(player, name);
                    return true;
                }
                if (args.length == 3 && args[1].equals("structure")) {
                    if (!YouerConfig.ban_structure_enable) {
                        sender.sendMessage(ChatColor.RED + check);
                        return false;
                    }
                    String name = args[2];
                    if (BanConfig.STRUCTURE.has(name)) {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.structure.exists"));
                        return false;
                    }
                    List<String> structureList = BuiltInRegistries.STRUCTURE_TYPE.keySet().stream().map(ResourceLocation::toString).toList();
                    if (!structureList.contains(name)) {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.structure.notexists"));
                        return false;
                    }
                    BanStructure.addBan(player, name);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                    return false;
                }
                switch (args[1]) {
                    case "item" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ITEM, I18n.as("banscmd.gui.add.item"));
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
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ITEM_MOSHOU, I18n.as("banscmd.gui.add.item.moshou"));
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
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ENTITY, I18n.as("banscmd.gui.add.entity"));
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
                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ENCHANTMENT, I18n.as("banscmd.gui.add.enchantment"));
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

                        if (args.length >= 3) {
                            String blockName = args[2];
                            Material material = Material.matchMaterial(blockName);

                            if (material == null) {
                                sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.block.invalid").formatted(blockName));
                                return false;
                            }

                            if (material.isAirSafe()) {
                                sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.block.air"));
                                return false;
                            }

                            String blockKey = material.getKey().asString();
                            List<String> old = BanConfig.getListByType(BanType.BLOCK);
                            if (old.contains(blockKey)) {
                                sender.sendMessage(ChatColor.YELLOW + I18n.as("banscmd.add.block.exists").formatted(blockKey));
                                return false;
                            }

                            old.add(blockKey);
                            BanUtils.saveToYaml(player, com.mohistmc.youer.feature.ban.ClickType.ADD, old, BanType.BLOCK);
                            sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.add.block.success").formatted(blockKey));
                            return true;
                        }

                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.BLOCK, I18n.as("banscmd.gui.add.block"));
                        Inventory inventory = banSaveInventory.getInventory();
                        player.openInventory(inventory);
                        BanListener.openInventory = banSaveInventory;
                        return true;
                    }
                    case "nbt" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }

                        ItemStack itemStack = player.getInventory().getItemInMainHand();
                        if (itemStack.isEmpty()) {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.nbt.noitem"));
                            return false;
                        }

                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.nbt.usage"));
                            return false;
                        }

                        String nbt = args[2];
                        BanConfig.NBT.addNbt(itemStack.getType().key().asString(), nbt);
                        sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.add.nbt.success").formatted(nbt));
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                        return false;
                    }
                }
            }
            case "show" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                    return false;
                }
                switch (args[1]) {
                    case "item" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.item"));
                        List<String> old = BanConfig.getListByType(BanType.ITEM);
                        for (String s : BanConfig.getListByType(BanType.ITEM)) {
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
                                            BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.ITEM);
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
                        List<String> old = BanConfig.getListByType(BanType.ITEM_MOSHOU);
                        for (String s : BanConfig.getListByType(BanType.ITEM_MOSHOU)) {
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
                                            BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.ITEM_MOSHOU);
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
                        List<String> old = BanConfig.getListByType(BanType.ENTITY);
                        for (String s : BanConfig.getListByType(BanType.ENTITY)) {
                            wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(EntityAPI.getType(s)))
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.ENTITY);
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
                        List<String> old = BanConfig.getListByType(BanType.ENCHANTMENT);
                        for (String s : BanConfig.getListByType(BanType.ENCHANTMENT)) {
                            wh.addItem(new GUIItem(new ItemStackFactory(Material.ENCHANTED_BOOK)
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .setEnchantment(ItemAPI.getEnchantmentByKey(s))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.ENCHANTMENT);
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
                        List<String> old = BanConfig.getListByType(BanType.RECIPE);
                        for (String s : BanConfig.getListByType(BanType.RECIPE)) {
                            wh.addItem(new GUIItem(new ItemStackFactory(Material.KNOWLEDGE_BOOK)
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.RECIPE);
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
                        List<String> old = BanConfig.getListByType(BanType.BLOCK);
                        for (String s : BanConfig.getListByType(BanType.BLOCK)) {
                            Material material = Material.matchMaterial(s);
                            if (material != null && !material.isAirSafe()) {
                                Material displayMaterial = material.isItem() ? material : Material.STRUCTURE_VOID;
                                wh.addItem(new GUIItem(new ItemStackFactory(displayMaterial)
                                        .setDisplayName(s)
                                        .addLore("§e" + I18n.as("banscmd.show.lore"))
                                        .build()) {
                                    @Override
                                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                        if (type.isRightClick()) {
                                            old.remove(s);
                                            BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.BLOCK);
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
                    case "nbt" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.nbt"));
                        for (String s : BanConfig.NBT.getAllNbtKeys()) {
                            Material material = Material.matchMaterial(s);
                            if (material != null && !material.isAirSafe()) {
                                wh.addItem(new GUIItem(new ItemStackFactory(material)
                                        .setDisplayName(s)
                                        .addLore("§e" + I18n.as("banscmd.show.nbt.lore"))
                                        .build()) {
                                    @Override
                                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                        if (type.isRightClick()) {
                                            wh.removeItem(this);
                                            BanConfig.NBT.clearNbt(s);
                                            wh.openGUI(player);
                                        }
                                    }
                                });
                            }
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "world" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.world"));
                        List<String> old = BanConfig.getListByType(BanType.WORLD);
                        for (String s : BanConfig.getListByType(BanType.WORLD)) {
                            wh.addItem(new GUIItem(new ItemStackFactory(Material.GRASS_BLOCK)
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.WORLD);
                                        wh.removeItem(this);
                                        wh.openGUI(player);
                                    }
                                }
                            });
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    case "structure" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.structure"));
                        List<String> old = BanConfig.getListByType(BanType.STRUCTURE);
                        for (String s : BanConfig.getListByType(BanType.STRUCTURE)) {
                            wh.addItem(new GUIItem(new ItemStackFactory(Material.STONE)
                                    .setDisplayName(s)
                                    .addLore("§e" + I18n.as("banscmd.show.lore"))
                                    .build()) {
                                @Override
                                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                    if (type.isRightClick()) {
                                        old.remove(s);
                                        BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, BanType.STRUCTURE);
                                        wh.removeItem(this);
                                        wh.openGUI(player);
                                    }
                                }
                            });
                        }
                        wh.openGUI(player);
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                        return false;
                    }
                }
            }
            case "setmessage" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
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
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.setmessage.noitem"));
                            return false;
                        }
                        if (BanItem.check(net.minecraft.world.item.ItemStack.fromBukkitCopy(itemStack)) || BanItem.checkMoShou(itemStack)) {
                            String result = Arrays.stream(args)
                                    .skip(2)
                                    .collect(Collectors.joining(" "));
                            BanConfig.BAN_MESSAGE.setBanMessage(itemStack.getType().name(), result);
                        } else {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.setmessage.success").formatted(itemStack.getType().name()));
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
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                        return false;
                    }
                }
            }
            default -> {
                sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
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

        if (args.length == 3 && args[0].equals("add") && args[1].equals("world") && (sender.isOp() || testPermission(sender))) {
            return BanWorld.CACHE.stream()
                    .map(ResourceLocation::toString)
                    .toList();
        }
        if (args.length == 3 && args[0].equals("add") && args[1].equals("structure") && (sender.isOp() || testPermission(sender))) {
            return BuiltInRegistries.STRUCTURE_TYPE.keySet().stream().map(ResourceLocation::toString).toList();
        }

        if (args.length == 3 && args[0].equals("add") && args[1].equals("block") && (sender.isOp() || testPermission(sender))) {
            return java.util.Arrays.stream(Material.values())
                    .filter(m -> m.isBlock() && !m.isAirSafe() && !m.isLegacy())
                    .map(m -> m.getKey().asString())
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        return list;
    }
}
