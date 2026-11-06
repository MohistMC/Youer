package com.mohistmc.youer.feature.ban;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.EntityAPI;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUI;
import com.mohistmc.youer.api.gui.GUIType;
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
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 5:33:43
 */
public class BansCommand extends Command {

    private final List<String> params = Arrays.asList("add", "show", "setmessage", "reload");
    private final List<String> params1 = Arrays.asList("item", "item-moshou", "entity", "enchantment", "recipe", "block", "nbt", "world", "structure", "effect", "command", "eat");

    public BansCommand(String name) {
        super(name);
        this.description = I18n.as("banscmd.description");
        this.usageMessage = "/bans [add|show|setmessage|reload] [item|item-moshou|entity|enchantment|recipe|block|world|structure]";
        this.setPermission("youer.command.bans");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
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
                    ResourceLocation recipeKey;
                    try {
                        recipeKey = ResourceLocation.parse(name);
                    } catch (RuntimeException ex) {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.recipe.notexists"));
                        return false;
                    }
                    if (!BanRecipe.CACHE.contains(recipeKey)) {
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
                    ResourceLocation worldKey;
                    try {
                        worldKey = ResourceLocation.parse(name);
                    } catch (RuntimeException ex) {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.world.notexists"));
                        return false;
                    }
                    if (!BanWorld.CACHE.contains(worldKey)) {
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
                    case "effect" -> {
                        if (!YouerConfig.ban_effect_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                            return false;
                        }
                        String effectName = args[2];
                        ResourceLocation effectKey;
                        try {
                            effectKey = ResourceLocation.parse(effectName);
                        } catch (RuntimeException ex) {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.effect.invalid").formatted(effectName));
                            return false;
                        }
                        if (!BuiltInRegistries.MOB_EFFECT.containsKey(effectKey)) {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.effect.invalid").formatted(effectName));
                            return false;
                        }
                        List<String> old = BanConfig.getListByType(BanType.EFFECT);
                        if (old.contains(effectName)) {
                            sender.sendMessage(ChatColor.YELLOW + I18n.as("banscmd.add.effect.exists").formatted(effectName));
                            return false;
                        }
                        old.add(effectName);
                        BanUtils.saveToYaml(player, com.mohistmc.youer.feature.ban.ClickType.ADD, old, BanType.EFFECT);
                        sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.add.effect.success").formatted(effectName));
                        return true;
                    }
                    case "eat" -> {
                        if (!YouerConfig.ban_eat_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        if (args.length < 3) {
                            BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.EAT, I18n.as("banscmd.gui.add.eat"));
                            Inventory inventory = banSaveInventory.getInventory();
                            player.openInventory(inventory);
                            BanListener.openInventory = banSaveInventory;
                            return true;
                        }
                        String eatName = args[2];
                        // 允许 modid:* 通配整个命名空间
                        if (!eatName.matches("^[a-z0-9_.-]+:\\*$")) {
                            Material material = Material.matchMaterial(eatName);
                            if (material == null || material.isAirSafe()) {
                                sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.eat.invalid").formatted(eatName));
                                return false;
                            }
                        }
                        List<String> old = BanConfig.getListByType(BanType.EAT);
                        if (old.contains(eatName)) {
                            sender.sendMessage(ChatColor.YELLOW + I18n.as("banscmd.add.eat.exists").formatted(eatName));
                            return false;
                        }
                        old.add(eatName);
                        BanUtils.saveToYaml(player, com.mohistmc.youer.feature.ban.ClickType.ADD, old, BanType.EAT);
                        sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.add.eat.success").formatted(eatName));
                        return true;
                    }
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

                        if (args.length >= 3) {
                            String entityName = args[2];
                            boolean isWildcard = entityName.matches("^[a-z0-9_.-]+:\\*$");
                            if (!isWildcard) {
                                ResourceLocation entityKey;
                                try {
                                    entityKey = ResourceLocation.parse(entityName);
                                } catch (RuntimeException ex) {
                                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.entity.invalid").formatted(entityName));
                                    return false;
                                }
                                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityKey)) {
                                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.entity.invalid").formatted(entityName));
                                    return false;
                                }
                            }

                            List<String> old = BanConfig.getListByType(BanType.ENTITY);
                            if (old.contains(entityName)) {
                                sender.sendMessage(ChatColor.YELLOW + I18n.as("banscmd.add.entity.exists").formatted(entityName));
                                return false;
                            }

                            old.add(entityName);
                            BanUtils.saveToYaml(player, com.mohistmc.youer.feature.ban.ClickType.ADD, old, BanType.ENTITY);
                            sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.add.entity.success").formatted(entityName));
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
                    case "command" -> {
                        if (!YouerConfig.ban_command_enable) {
                            sender.sendMessage(ChatColor.RED + check);
                            return false;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                            return false;
                        }
                        String commandName = args[2].toLowerCase(Locale.ENGLISH);
                        // Strip leading slash if present
                        if (commandName.startsWith("/")) {
                            commandName = commandName.substring(1);
                        }
                        // Strip minecraft: prefix for consistency
                        if (commandName.startsWith("minecraft:")) {
                            commandName = commandName.substring(10);
                        }
                        // Prevent banning the bans command itself
                        if (commandName.equals("bans") || commandName.equals("youer:bans")) {
                            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.add.command.self"));
                            return false;
                        }
                        List<String> old = BanConfig.getListByType(BanType.COMMAND);
                        if (old.contains(commandName)) {
                            sender.sendMessage(ChatColor.YELLOW + I18n.as("banscmd.add.command.exists").formatted(commandName));
                            return false;
                        }
                        old.add(commandName);
                        BanUtils.saveToYaml(player, com.mohistmc.youer.feature.ban.ClickType.ADD, old, BanType.COMMAND);
                        sender.sendMessage(ChatColor.GREEN + I18n.as("banscmd.add.command.success").formatted(commandName));
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                        return false;
                    }
                }
            }
            case "show" -> {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                    return false;
                }
                openTypeGUI(player);
                return true;
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

    private void openTypeGUI(Player player) {
        boolean showAll = player.isOp();
        DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.type"));
        for (String banType : params1) {
            int count = showCount(banType);
            if (!showAll && (count == 0 || !isBanEnabled(banType))) {
                continue;
            }
            wh.addItem(new GUIItem(new ItemStackFactory(showIcon(banType))
                    .setDisplayName(I18n.as("banscmd.show." + banType))
                    .addLore("§e" + I18n.as("banscmd.show.type.lore"))
                    .addLore("§7" + I18n.as("banscmd.show.type.count", count))
                    .build()) {
                @Override
                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                    if (type.isLeftClick() && !type.isShiftClick()) {
                        openShowGUI(u, banType);
                    }
                }
            });
        }
        if (wh.getItems().isEmpty()) {
            wh.addItem(new GUIItem(new ItemStackFactory(Material.BARRIER)
                    .setDisplayName(I18n.as("banscmd.show.type.empty"))
                    .build()));
        }
        wh.openGUI(player);
    }

    private void openShowGUI(Player player, String banType) {
        boolean canDelete = player.isOp();
        switch (banType) {
            case "effect" -> {
                DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.effect"));
                List<String> old = BanConfig.getListByType(BanType.EFFECT);
                for (String s : BanConfig.getListByType(BanType.EFFECT)) {
                    wh.addItem(new GUIItem(new ItemStackFactory(Material.POTION)
                            .setDisplayName(s)
                            .addLore("§e" + I18n.as("banscmd.show.lore"))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.EFFECT, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
            }
            case "eat" -> {
                DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.eat"));
                List<String> old = BanConfig.getListByType(BanType.EAT);
                for (String s : BanConfig.getListByType(BanType.EAT)) {
                    Material material = Material.matchMaterial(s);
                    // 通配条目（modid:*）没有对应 Material，用纸显示 id
                    ItemStackFactory factory = (material != null && !material.isAirSafe())
                            ? new ItemStackFactory(material)
                            : new ItemStackFactory(Material.PAPER).setDisplayName(s);
                    wh.addItem(new GUIItem(factory
                            .addLore("§e" + I18n.as("banscmd.show.lore"))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.EAT, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
            }
            case "item" -> {
                DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.item"));
                List<String> old = BanConfig.getListByType(BanType.ITEM);
                for (String s : BanConfig.getListByType(BanType.ITEM)) {
                    Material material = Material.matchMaterial(s);
                    if (material != null && !material.isAirSafe()) {
                        wh.addItem(new GUIItem(new ItemStackFactory(material)
                                .addLore("§e" + I18n.as("banscmd.show.lore"))
                                .build()) {
                            @Override
                            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                if (type.isRightClick() && u.isOp()) {
                                    requestRemove(u, s, itemStack, old, BanType.ITEM, wh, this);
                                }
                            }
                        });
                    }
                }
                prepareShow(wh, player, canDelete);
            }
            case "item-moshou" -> {
                DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.item-moshou"));
                List<String> old = BanConfig.getListByType(BanType.ITEM_MOSHOU);
                for (String s : BanConfig.getListByType(BanType.ITEM_MOSHOU)) {
                    Material material = Material.matchMaterial(s);
                    if (material != null && !material.isAirSafe()) {
                        wh.addItem(new GUIItem(new ItemStackFactory(material)
                                .addLore("§e" + I18n.as("banscmd.show.lore"))
                                .build()) {
                            @Override
                            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                if (type.isRightClick() && u.isOp()) {
                                    requestRemove(u, s, itemStack, old, BanType.ITEM_MOSHOU, wh, this);
                                }
                            }
                        });
                    }
                }
                prepareShow(wh, player, canDelete);
            }
            case "entity" -> {
                DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.entity"));
                List<String> old = BanConfig.getListByType(BanType.ENTITY);
                for (String s : BanConfig.getListByType(BanType.ENTITY)) {
                    net.minecraft.world.entity.EntityType<?> type;
                    try {
                        type = EntityAPI.getType(s);
                    } catch (RuntimeException ex) {
                        type = null; // Wildcard (modid:*) or unknown entries cannot be parsed as ResourceLocation
                    }
                    wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(type))
                            .addLore("§e" + I18n.as("banscmd.show.lore"))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.ENTITY, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
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
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.ENCHANTMENT, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
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
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.RECIPE, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
            }
            case "block" -> {
                DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.block"));
                List<String> old = BanConfig.getListByType(BanType.BLOCK);
                for (String s : BanConfig.getListByType(BanType.BLOCK)) {
                    Material material = Material.matchMaterial(s);
                    if (material != null && !material.isAirSafe()) {
                        Material displayMaterial = material.isItem() ? material : Material.STRUCTURE_VOID;
                        wh.addItem(new GUIItem(new ItemStackFactory(displayMaterial)
                                .addLore("§e" + I18n.as("banscmd.show.lore"))
                                .build()) {
                            @Override
                            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                if (type.isRightClick() && u.isOp()) {
                                    requestRemove(u, s, itemStack, old, BanType.BLOCK, wh, this);
                                }
                            }
                        });
                    }
                }
                prepareShow(wh, player, canDelete);
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
                                if (type.isRightClick() && u.isOp()) {
                                    requestRemoveNbt(u, s, itemStack, wh, this);
                                }
                            }
                        });
                    }
                }
                prepareShow(wh, player, canDelete);
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
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.WORLD, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
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
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.STRUCTURE, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
            }
            case "command" -> {
                DemoGUI wh = new DemoGUI(I18n.as("banscmd.show.command"));
                List<String> old = BanConfig.getListByType(BanType.COMMAND);
                for (String s : BanConfig.getListByType(BanType.COMMAND)) {
                    wh.addItem(new GUIItem(new ItemStackFactory(Material.COMMAND_BLOCK)
                            .setDisplayName("/" + s)
                            .addLore("§e" + I18n.as("banscmd.show.lore"))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            if (type.isRightClick() && u.isOp()) {
                                requestRemove(u, s, itemStack, old, BanType.COMMAND, wh, this);
                            }
                        }
                    });
                }
                prepareShow(wh, player, canDelete);
            }
            default -> {
                player.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
            }
        }
    }

    private void prepareShow(DemoGUI wh, Player player, boolean canDelete) {
        String hint = "§e" + I18n.as("banscmd.show.lore");
        for (GUIItem item : wh.getItems()) {
            ItemStack stack = item.getDisplay();
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) {
                continue;
            }
            List<String> current = meta.getLore();
            List<String> lore = current == null ? new ArrayList<>() : new ArrayList<>(current);
            lore.remove(hint);
            if (canDelete) {
                lore.add(hint);
            }
            meta.setLore(lore.isEmpty() ? null : lore);
            stack.setItemMeta(meta);
        }
        wh.setBottomItem(45, new GUIItem(new ItemStackFactory(Material.ARROW)
                .setDisplayName(I18n.as("banscmd.show.type.home"))
                .addLore("§7" + I18n.as("banscmd.show.type.home.lore"))
                .build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                if (!type.isShiftClick()) {
                    openTypeGUI(u);
                }
            }
        });
        wh.openGUI(player);
    }

    private static void requestRemove(Player u, String entry, ItemStack display, List<String> old, BanType banType, DemoGUI wh, GUIItem item) {
        confirmRemove(u, entry, display, () -> {
            old.remove(entry);
            BanUtils.saveToYaml(u, com.mohistmc.youer.feature.ban.ClickType.REMOVE, old, banType);
            wh.removeItem(item);
            wh.openGUI(u);
        }, () -> wh.openGUI(u));
    }

    private static void requestRemoveNbt(Player u, String entry, ItemStack display, DemoGUI wh, GUIItem item) {
        confirmRemove(u, entry, display, () -> {
            wh.removeItem(item);
            BanConfig.NBT.clearNbt(entry);
            wh.openGUI(u);
        }, () -> wh.openGUI(u));
    }

    private static void confirmRemove(Player p, String entry, ItemStack display, Runnable confirm, Runnable cancel) {
        GUI gui = new GUI(GUIType.THREEBYNINE, I18n.as("banscmd.confirm.title"));
        gui.setItem(13, new GUIItem(new ItemStackFactory(display)
                .setDisplayName(entry)
                .build()));
        gui.setItem(11, new GUIItem(new ItemStackFactory(Material.LIME_CONCRETE)
                .setDisplayName(I18n.as("banscmd.confirm.yes"))
                .build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                if (type.isLeftClick() && !type.isShiftClick() && u.isOp()) {
                    confirm.run();
                }
            }
        });
        gui.setItem(15, new GUIItem(new ItemStackFactory(Material.RED_CONCRETE)
                .setDisplayName(I18n.as("banscmd.confirm.no"))
                .build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                if (type.isLeftClick() && !type.isShiftClick()) {
                    cancel.run();
                }
            }
        });
        gui.openGUI(p);
    }

    private static Material showIcon(String banType) {
        return switch (banType) {
            case "item" -> Material.DIAMOND_SWORD;
            case "item-moshou" -> Material.NETHER_STAR;
            case "entity" -> Material.CREEPER_SPAWN_EGG;
            case "enchantment" -> Material.ENCHANTED_BOOK;
            case "recipe" -> Material.KNOWLEDGE_BOOK;
            case "block" -> Material.STONE;
            case "nbt" -> Material.NAME_TAG;
            case "world" -> Material.GRASS_BLOCK;
            case "structure" -> Material.STRUCTURE_BLOCK;
            case "effect" -> Material.POTION;
            case "command" -> Material.COMMAND_BLOCK;
            case "eat" -> Material.APPLE;
            default -> Material.PAPER;
        };
    }

    private static int showCount(String banType) {
        return switch (banType) {
            case "item" -> BanConfig.getSetByType(BanType.ITEM).size();
            case "item-moshou" -> BanConfig.getSetByType(BanType.ITEM_MOSHOU).size();
            case "entity" -> BanConfig.getSetByType(BanType.ENTITY).size();
            case "enchantment" -> BanConfig.getSetByType(BanType.ENCHANTMENT).size();
            case "recipe" -> BanConfig.getSetByType(BanType.RECIPE).size();
            case "block" -> BanConfig.getSetByType(BanType.BLOCK).size();
            case "nbt" -> BanConfig.NBT.getAllNbtKeys().size();
            case "world" -> BanConfig.getSetByType(BanType.WORLD).size();
            case "structure" -> BanConfig.getSetByType(BanType.STRUCTURE).size();
            case "effect" -> BanConfig.getSetByType(BanType.EFFECT).size();
            case "command" -> BanConfig.getSetByType(BanType.COMMAND).size();
            case "eat" -> BanConfig.getSetByType(BanType.EAT).size();
            default -> 0;
        };
    }

    private static boolean isBanEnabled(String banType) {
        return switch (banType) {
            case "item", "item-moshou", "nbt" -> YouerConfig.ban_item_enable;
            case "entity" -> YouerConfig.ban_entity_enable;
            case "enchantment" -> YouerConfig.ban_enchantment_enable;
            case "recipe" -> YouerConfig.ban_recipe_enable;
            case "block" -> YouerConfig.ban_block_enable;
            case "world" -> YouerConfig.ban_world_enable;
            case "structure" -> YouerConfig.ban_structure_enable;
            case "effect" -> YouerConfig.ban_effect_enable;
            case "command" -> YouerConfig.ban_command_enable;
            case "eat" -> YouerConfig.ban_eat_enable;
            default -> true;
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && (sender.isOp() || testPermission(sender))) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("show") && (sender.isOp() || testPermission(sender))) {
            for (String param : params1) {
                if (param.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(param);
                }
            }
        }
        if (args.length == 3 && args[0].equals("add") && args[1].equals("recipe") && (sender.isOp() || testPermission(sender))) {
            return BanRecipe.CACHE.stream()
                    .map(ResourceLocation::toString)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equals("add") && args[1].equals("world") && (sender.isOp() || testPermission(sender))) {
            return BanWorld.CACHE.stream()
                    .map(ResourceLocation::toString)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equals("add") && args[1].equals("structure") && (sender.isOp() || testPermission(sender))) {
            return BuiltInRegistries.STRUCTURE_TYPE.keySet().stream().map(ResourceLocation::toString)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equals("add") && args[1].equals("effect") && (sender.isOp() || testPermission(sender))) {
            return BuiltInRegistries.MOB_EFFECT.keySet().stream()
                    .map(ResourceLocation::toString)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equals("add") && args[1].equals("entity") && (sender.isOp() || testPermission(sender))) {
            java.util.Set<String> suggestions = new java.util.HashSet<>();
            for (var key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                String name = key.toString();
                if (name.toLowerCase().startsWith(args[2].toLowerCase())) {
                    suggestions.add(name);
                }
            }
            // Mod wildcard suggestions (modid:*)
            java.util.Set<String> namespaces = new java.util.HashSet<>();
            for (var key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                namespaces.add(key.getNamespace());
            }
            for (String ns : namespaces) {
                String wildcard = ns + ":*";
                if (wildcard.toLowerCase().startsWith(args[2].toLowerCase())) {
                    suggestions.add(wildcard);
                }
            }
            List<String> sorted = new ArrayList<>(suggestions);
            java.util.Collections.sort(sorted);
            return sorted.stream().limit(50).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equals("add") && args[1].equals("block") && (sender.isOp() || testPermission(sender))) {
            return java.util.Arrays.stream(Material.values())
                    .filter(m -> m.isBlock() && !m.isAirSafe() && !m.isLegacy())
                    .map(m -> m.getKey().asString())
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equals("add") && args[1].equals("eat") && (sender.isOp() || testPermission(sender))) {
            return java.util.Arrays.stream(Material.values())
                    .filter(m -> m.isEdible() && !m.isLegacy())
                    .map(m -> m.getKey().asString())
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        return list;
    }
}
