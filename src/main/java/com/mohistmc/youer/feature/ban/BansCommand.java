package com.mohistmc.youer.feature.ban;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.EntityAPI;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.feature.ban.bans.BanItem;
import com.mohistmc.youer.feature.ban.bans.BanRecipe;
import com.mohistmc.youer.feature.ban.bans.BanWorld;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
    private final List<String> params1 = Arrays.asList("item", "item-moshou", "entity", "enchantment", "recipe", "block", "nbt", "world");

    public BansCommand(String name) {
        super(name);
        this.description = I18n.as("banscmd.description");
        this.usageMessage = "/bans [add|show|setmessage] [item|item-moshou|entity|enchantment|recipe|block|world]";
        this.setPermission("youer.command.bans");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }
        String check = I18n.as("banscmd.check");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.as("banscmd.error.notplayer"));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
            return false;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "add" -> {
                if (args.length == 3 && args[1].equals("recipe")) {
                    if (!YouerConfig.ban_recipe_enable) {
                        sender.sendMessage(check);
                        return false;
                    }
                    String name = args[2];
                    if (BanConfig.RECIPE.has(name)) {
                        sender.sendMessage(I18n.as("banscmd.add.recipe.exists"));
                        return false;
                    }
                    if (!BanRecipe.CACHE.contains(Identifier.parse(name))) {
                        sender.sendMessage(I18n.as("banscmd.add.recipe.notexists"));
                        return false;
                    }
                    BanRecipe.addBan(player, name);
                    return true;
                }
                if (args.length == 3 && args[1].equals("world")) {
                    if (!YouerConfig.ban_world_enable) {
                        sender.sendMessage(check);
                        return false;
                    }
                    String name = args[2];
                    if (BanConfig.WORLD.has(name)) {
                        sender.sendMessage(I18n.as("banscmd.add.world.exists"));
                        return false;
                    }
                    if (!BanWorld.CACHE.contains(Identifier.parse(name))) {
                        sender.sendMessage(I18n.as("banscmd.add.world.notexists"));
                        return false;
                    }
                    BanWorld.addBan(player, name);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
                    return false;
                }
                switch (args[1]) {
                    case "item" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(check);
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
                            sender.sendMessage(check);
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
                            sender.sendMessage(check);
                            return false;
                        }

                        if (args.length >= 3) {
                            String entityName = args[2];
                            if (BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityName)).isEmpty()) {
                                sender.sendMessage(I18n.as("banscmd.add.entity.invalid").formatted(entityName));
                                return false;
                            }

                            String entityKey = Identifier.parse(entityName).toString();
                            List<String> old = BanConfig.getListByType(BanType.ENTITY);
                            if (old.contains(entityKey)) {
                                sender.sendMessage(ChatColor.YELLOW + I18n.as("banscmd.add.entity.exists").formatted(entityKey));
                                return false;
                            }

                            old.add(entityKey);
                            BanConfig.saveToYaml(player, com.mohistmc.youer.feature.ban.ClickType.ADD, old, BanType.ENTITY);
                            sender.sendMessage(I18n.as("banscmd.add.entity.success").formatted(entityKey));
                            return true;
                        }

                        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.ENTITY, I18n.as("banscmd.gui.add.entity"));
                        Inventory inventory = banSaveInventory.getInventory();
                        player.openInventory(inventory);
                        BanListener.openInventory = banSaveInventory;
                        return true;
                    }
                    case "enchantment" -> {
                        if (!YouerConfig.ban_enchantment_enable) {
                            sender.sendMessage(check);
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
                            sender.sendMessage(check);
                            return false;
                        }

                        if (args.length >= 3) {
                            String blockName = args[2];
                            Material material = Material.matchMaterial(blockName);

                            if (material == null) {
                                sender.sendMessage(I18n.as("banscmd.add.block.invalid").formatted(blockName));
                                return false;
                            }

                            if (material.isAir()) {
                                sender.sendMessage(I18n.as("banscmd.add.block.air"));
                                return false;
                            }

                            String blockKey = material.getKey().asString();
                            List<String> old = BanConfig.getListByType(BanType.BLOCK);
                            if (old.contains(blockKey)) {
                                sender.sendMessage(ChatColor.YELLOW + I18n.as("banscmd.add.block.exists").formatted(blockKey));
                                return false;
                            }

                            old.add(blockKey);
                            BanConfig.saveToYaml(player, com.mohistmc.youer.feature.ban.ClickType.ADD, old, BanType.BLOCK);
                            sender.sendMessage(I18n.as("banscmd.add.block.success").formatted(blockKey));
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
                            sender.sendMessage(check);
                            return false;
                        }

                        ItemStack itemStack = player.getInventory().getItemInMainHand();
                        if (itemStack.isEmpty()) {
                            sender.sendMessage(I18n.as("banscmd.add.nbt.noitem"));
                            return false;
                        }

                        if (args.length < 3) {
                            sender.sendMessage(I18n.as("banscmd.add.nbt.usage"));
                            return false;
                        }

                        String nbt = args[2];
                        BanConfig.NBT.addNbt(itemStack.getType().key().asString(), nbt);
                        sender.sendMessage(I18n.as("banscmd.add.nbt.success").formatted(nbt));
                        return true;
                    }
                    default -> {
                        sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
                        return false;
                    }
                }
            }
            case "show" -> {
                if (args.length != 2) {
                    sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
                    return false;
                }
                switch (args[1]) {
                    case "item" -> showBanList(player, BanType.ITEM, BanConfig.ITEM,
                            "banscmd.show.item", s -> new ItemStackFactory(Material.matchMaterial(s)));
                    case "item-moshou" -> showBanList(player, BanType.ITEM_MOSHOU, BanConfig.ITEM_MOSHOU,
                            "banscmd.show.item-moshou", s -> new ItemStackFactory(Material.matchMaterial(s)));
                    case "entity" -> showBanList(player, BanType.ENTITY, BanConfig.ENTITY,
                            "banscmd.show.entity", s -> new ItemStackFactory(ItemAPI.getEggMaterial(EntityAPI.getType(s))));
                    case "enchantment" -> showBanList(player, BanType.ENCHANTMENT, BanConfig.ENCHANTMENT,
                            "banscmd.show.enchantment", s -> new ItemStackFactory(Material.ENCHANTED_BOOK)
                            .setEnchantment(ItemAPI.getEnchantmentByKey(s)));
                    case "recipe" -> showBanList(player, BanType.RECIPE, BanConfig.RECIPE,
                            "banscmd.show.recipe", s -> new ItemStackFactory(Material.KNOWLEDGE_BOOK));
                    case "block" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.block"));
                        List<String> old = BanConfig.getListByType(BanType.BLOCK);
                        for (String s : BanConfig.getListByType(BanType.BLOCK)) {
                            Material m = Material.matchMaterial(s);
                            if (m == null || m.isAir()) continue;
                            addBanItem(wh, old, BanType.BLOCK, BanConfig.BLOCK, s, player,
                                    new ItemStackFactory(m.isItem() ? m : Material.STRUCTURE_VOID));
                        }
                        wh.openGUI(player);
                    }
                    case "world" -> showBanList(player, BanType.WORLD, BanConfig.WORLD,
                            "banscmd.show.world", s -> new ItemStackFactory(Material.GRASS_BLOCK));
                    case "nbt" -> {
                        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.nbt"));
                        for (String s : BanConfig.NBT.getAllNbtKeys()) {
                            Material m = Material.matchMaterial(s);
                            if (m == null || m.isAir()) continue;
                            wh.addItem(new GUIItem(new ItemStackFactory(m)
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
                        wh.openGUI(player);
                    }
                    default -> {
                        sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
                        return false;
                    }
                }
                return true;
            }
            case "setmessage" -> {
                if (args.length < 2) {
                    sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
                    return false;
                }
                switch (args[1]) {
                    case "item" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(check);
                            return false;
                        }
                        ItemStack itemStack = player.getInventory().getItemInMainHand();
                        if (itemStack.isEmpty()) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.noitem"));
                            return false;
                        }
                        if (BanItem.check(itemStack) || BanItem.checkMoShou(itemStack)) {
                            String result = Arrays.stream(args)
                                    .skip(2)
                                    .collect(Collectors.joining(" "));
                            BanConfig.ITEM.setMessage(itemStack.getType().getKey().asString(), result);
                        } else {
                            sender.sendMessage(I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        sender.sendMessage(I18n.as("banscmd.setmessage.success").formatted(itemStack.getType().getKey().asString()));
                        return true;
                    }
                    case "item-moshou" -> {
                        if (!YouerConfig.ban_item_enable) {
                            sender.sendMessage(check);
                            return false;
                        }
                        ItemStack itemStack = player.getInventory().getItemInMainHand();
                        if (itemStack.isEmpty()) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.noitem"));
                            return false;
                        }
                        if (BanItem.check(itemStack) || BanItem.checkMoShou(itemStack)) {
                            String result = Arrays.stream(args)
                                    .skip(2)
                                    .collect(Collectors.joining(" "));
                            BanConfig.ITEM_MOSHOU.setMessage(itemStack.getType().getKey().asString(), result);
                        } else {
                            sender.sendMessage(I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        sender.sendMessage(I18n.as("banscmd.setmessage.success").formatted(itemStack.getType().getKey().asString()));
                        return true;
                    }
                    case "entity" -> {
                        if (!YouerConfig.ban_entity_enable) {
                            sender.sendMessage(check);
                            return false;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(I18n.as("banscmd.usage.format", "/bans setmessage entity <entity_key> <message...>"));
                            return false;
                        }
                        String key = args[2];
                        if (!BanConfig.ENTITY.has(key)) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        String msg = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
                        BanConfig.ENTITY.setMessage(key, msg);
                        sender.sendMessage(I18n.as("banscmd.setmessage.success").formatted(key));
                        return true;
                    }
                    case "enchantment" -> {
                        if (!YouerConfig.ban_enchantment_enable) {
                            sender.sendMessage(check);
                            return false;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(I18n.as("banscmd.usage.format", "/bans setmessage enchantment <ench_key> <message...>"));
                            return false;
                        }
                        String key = args[2];
                        if (!BanConfig.ENCHANTMENT.has(key)) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        String msg = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
                        BanConfig.ENCHANTMENT.setMessage(key, msg);
                        sender.sendMessage(I18n.as("banscmd.setmessage.success").formatted(key));
                        return true;
                    }
                    case "recipe" -> {
                        if (!YouerConfig.ban_recipe_enable) {
                            sender.sendMessage(check);
                            return false;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(I18n.as("banscmd.usage.format", "/bans setmessage recipe <recipe_key> <message...>"));
                            return false;
                        }
                        String key = args[2];
                        if (!BanConfig.RECIPE.has(key)) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        String msg = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
                        BanConfig.RECIPE.setMessage(key, msg);
                        sender.sendMessage(I18n.as("banscmd.setmessage.success").formatted(key));
                        return true;
                    }
                    case "block" -> {
                        if (!YouerConfig.ban_block_enable) {
                            sender.sendMessage(check);
                            return false;
                        }
                        ItemStack itemStack = player.getInventory().getItemInMainHand();
                        if (itemStack.isEmpty()) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.noitem"));
                            return false;
                        }
                        String key = itemStack.getType().getKey().asString();
                        if (!BanConfig.BLOCK.has(key)) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        String msg = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
                        BanConfig.BLOCK.setMessage(key, msg);
                        sender.sendMessage(I18n.as("banscmd.setmessage.success").formatted(key));
                        return true;
                    }
                    case "world" -> {
                        if (!YouerConfig.ban_world_enable) {
                            sender.sendMessage(check);
                            return false;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(I18n.as("banscmd.usage.format", "/bans setmessage world <world_key> <message...>"));
                            return false;
                        }
                        String key = args[2];
                        if (!BanConfig.WORLD.has(key)) {
                            sender.sendMessage(I18n.as("banscmd.setmessage.notbanned"));
                            return false;
                        }
                        String msg = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
                        BanConfig.WORLD.setMessage(key, msg);
                        sender.sendMessage(I18n.as("banscmd.setmessage.success").formatted(key));
                        return true;
                    }
                    default -> {
                        sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
                        return false;
                    }
                }
            }
            default -> {
                sender.sendMessage(I18n.as("banscmd.usage.format", usageMessage));
                return false;
            }
        }
    }

    // === Show helpers ===

    private void showBanList(Player player, BanType banType, BanConfig msgConfig, String titleKey,
                             Function<String, ItemStackFactory> factoryBuilder) {
        DemoGUI wh = new DemoGUI(I18n.as(titleKey));
        List<String> values = BanConfig.getListByType(banType);
        for (String s : values) {
            ItemStackFactory factory = factoryBuilder.apply(s);
            if (factory == null) continue;
            addBanItem(wh, values, banType, msgConfig, s, player, factory);
        }
        wh.openGUI(player);
    }

    private void addBanItem(DemoGUI wh, List<String> values, BanType banType, BanConfig msgConfig,
                            String key, Player player, ItemStackFactory factory) {
        factory.setLore(List.of("", "§e" + I18n.as("banscmd.show.lore"), ""));
        String msg = msgConfig.getMessage(key);
        if (!msg.isEmpty()) factory.addLore("§7" + msg);
        wh.addItem(new GUIItem(factory.build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                if (type.isRightClick()) {
                    values.remove(key);
                    BanConfig.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, values, banType);
                    wh.removeItem(this);
                    wh.openGUI(player);
                }
            }
        });
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
                    .map(Identifier::toString)
                    .toList();
        }

        if (args.length == 3 && args[0].equals("add") && args[1].equals("world") && (sender.isOp() || testPermission(sender))) {
            return BanWorld.CACHE.stream()
                    .map(Identifier::toString)
                    .toList();
        }

        if (args.length == 3 && args[0].equals("add") && args[1].equals("block") && (sender.isOp() || testPermission(sender))) {
            return java.util.Arrays.stream(Material.values())
                    .filter(m -> m.isBlock() && !m.isAir() && !m.isLegacy())
                    .map(m -> m.getKey().asString())
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equals("add") && args[1].equals("entity") && (sender.isOp() || testPermission(sender))) {
            List<String> completions = new ArrayList<>();
            for (var entityType : BuiltInRegistries.ENTITY_TYPE) {
                Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                if (key != null && key.toString().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(key.toString());
                }
            }
            return completions.stream().limit(50).collect(Collectors.toList());
        }

        return list;
    }
}
