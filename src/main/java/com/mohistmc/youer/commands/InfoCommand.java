
package com.mohistmc.youer.commands;

import com.mohistmc.youer.api.ColorAPI;
import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul by MohistMC
 * @date 2025/8/31 20:00:00
 */
public class InfoCommand extends Command {

    private final List<String> params = List.of("item", "block", "entity", "cmd", "item-component");

    public InfoCommand(String name) {
        super(name);
        this.description = "Youer infos commands";
        this.usageMessage = "/infos [item|block|entity|cmd|item-component]";
        this.setPermission("youer.command.infos");
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
        } else if (args.length == 2 && "cmd".equalsIgnoreCase(args[0]) && (sender.isOp() || testPermission(sender))) {
            for (String command : Bukkit.getServer().getCommandMap().getKnownCommands().keySet()) {
                if (command.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(command);
                }
            }
        }

        return list;
    }


    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(I18n.as("commands.usage", usageMessage));
            return false;
        }


        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.as("error.notplayer"));
            return false;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "item" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                ItemsCommand.info(player);
                return true;
            }
            case "block" -> {
                Block block = player.getTargetBlockExact(5);
                if (block == null) {
                    sender.sendMessage(I18n.as("info.block.not_found"));
                    return false;
                } else {
                    PlayerAPI.sendMessageByCopy(player, I18n.as("info.block.type", block.getType().name()), block.getType().name());
                    PlayerAPI.sendMessageByCopy(player, I18n.as("info.block.key", block.getType().getKey().asString()), block.getType().getKey().asString());
                    sender.sendMessage(I18n.as("info.block.data", block.getBlockData().getAsString()));
                    sender.sendMessage(I18n.as("info.block.pos", block.getX() + ", " + block.getY() + ", " + block.getZ()));
                    sender.sendMessage(I18n.as("info.block.world", block.getWorld().getName()));
                    sender.sendMessage(I18n.as("info.block.light", block.getLightLevel()));
                    sender.sendMessage(I18n.as("info.block.biome", block.getBiome().name() + "(%s)".formatted(Component.translatable(block.getBiome().translationKey()).getString())));
                    sender.sendMessage(I18n.as("info.block.hardness", block.getType().getHardness()));
                    sender.sendMessage(I18n.as("info.block.resistance", block.getType().getBlastResistance()));
                    sender.sendMessage(I18n.as("info.block.slipperiness", block.getType().getSlipperiness()));
                    sender.sendMessage(I18n.as("info.block.replaceable", block.isReplaceable()));
                    sender.sendMessage(I18n.as("info.block.requires_tool", block.getBlockData().requiresCorrectToolForDrops()));

                    sender.sendMessage(I18n.as("info.block.solid", I18n.as(block.getType().isSolid() ? "info.yes" : "info.no")));
                    sender.sendMessage(I18n.as("info.block.burnable", I18n.as(block.getType().isBurnable() ? "info.yes" : "info.no")));
                    sender.sendMessage(I18n.as("info.block.interactable", I18n.as(block.getType().isInteractable() ? "info.yes" : "info.no")));
                    sender.sendMessage(I18n.as("info.block.gravity", I18n.as(block.getType().hasGravity() ? "info.yes" : "info.no")));

                    if (block.getState() != null) {
                        sender.sendMessage(I18n.as("info.block.state", block.getState().getClass().getSimpleName()));
                    }
                }
                return true;
            }
            case "entity" -> {
                Entity entity = player.getTargetEntity(5, true);
                if (entity == null) {
                    sender.sendMessage(I18n.as("info.entity.not_found"));
                    return false;
                } else {
                    PlayerAPI.sendMessageByCopy(player, I18n.as("info.entity.type", entity.getType().name()), entity.getType().name());
                    PlayerAPI.sendMessageByCopy(player, I18n.as("info.entity.key", entity.getType().getKey().asString()), entity.getType().getKey().asString());
                    PlayerAPI.sendMessageByCopy(player, I18n.as("info.entity.uuid", entity.getUniqueId().toString()), entity.getUniqueId().toString());
                    sender.sendMessage(I18n.as("info.entity.pos", String.format("%.2f, %.2f, %.2f", entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ())));
                    sender.sendMessage(I18n.as("info.entity.world", entity.getWorld().getName()));
                    sender.sendMessage(I18n.as("info.entity.health", entity instanceof org.bukkit.entity.Damageable ? String.valueOf(((org.bukkit.entity.Damageable) entity).getHealth()) : "N/A"));
                    sender.sendMessage(I18n.as("info.entity.onground", entity.isOnGround()));
                    sender.sendMessage(I18n.as("info.entity.burning", entity.isVisualFire()));
                    sender.sendMessage(I18n.as("info.entity.ticks", entity.getTicksLived() + " ticks"));
                    sender.sendMessage(I18n.as("info.entity.frozen", entity.isFrozen()));
                    sender.sendMessage(I18n.as("info.entity.gravity", entity.hasGravity()));

                    if (entity.getCustomName() != null) {
                        sender.sendMessage(I18n.as("info.entity.custom_name", entity.getCustomName()));
                    }

                    if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
                        sender.sendMessage(I18n.as("info.entity.max_health", livingEntity.getMaxHealth()));
                        sender.sendMessage(I18n.as("info.entity.ai", livingEntity.hasAI()));
                        sender.sendMessage(I18n.as("info.entity.sleeping", livingEntity.isSleeping()));

                        if (livingEntity instanceof Player targetPlayer) {
                            sender.sendMessage(I18n.as("info.entity.player_info"));
                            sender.sendMessage(I18n.as("info.entity.gamemode", targetPlayer.getGameMode().name()));
                            sender.sendMessage(I18n.as("info.entity.level", targetPlayer.getLevel()));
                            sender.sendMessage(I18n.as("info.entity.exp", targetPlayer.getTotalExperience()));
                            sender.sendMessage(I18n.as("info.entity.food", targetPlayer.getFoodLevel()));
                            sender.sendMessage(I18n.as("info.entity.saturation", targetPlayer.getSaturation()));
                        }

                        sender.sendMessage(I18n.as("info.entity.equipment"));
                        if (livingEntity.getEquipment() != null) {
                            sender.sendMessage(I18n.as("info.entity.main_hand", livingEntity.getEquipment().getItemInMainHand().getType() != Material.AIR ? livingEntity.getEquipment().getItemInMainHand().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(I18n.as("info.entity.off_hand", livingEntity.getEquipment().getItemInOffHand().getType() != Material.AIR ? livingEntity.getEquipment().getItemInOffHand().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(I18n.as("info.entity.helmet", livingEntity.getEquipment().getHelmet() != null ? livingEntity.getEquipment().getHelmet().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(I18n.as("info.entity.chestplate", livingEntity.getEquipment().getChestplate() != null ? livingEntity.getEquipment().getChestplate().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(I18n.as("info.entity.leggings", livingEntity.getEquipment().getLeggings() != null ? livingEntity.getEquipment().getLeggings().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(I18n.as("info.entity.boots", livingEntity.getEquipment().getBoots() != null ? livingEntity.getEquipment().getBoots().getType().name() : I18n.as("info.entity.empty")));
                        }
                    }
                }
                return true;
            }
            case "item-component" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
                DataComponentMap components = nmsItem.getComponents();

                DemoGUI gui = new DemoGUI(I18n.as("info.item_component.title"));

                for (DataComponentType<?> type : components.keySet()) {
                    String typeName = type.toString();
                    Object value = components.get(type);
                    String valueStr = String.valueOf(value);

                    gui.addItem(new GUIItem(new ItemStackFactory(Material.KNOWLEDGE_BOOK)
                            .setDisplayName("§e" + typeName)
                            .addLore(I18n.as("info.item_component.value", valueStr))
                            .addLore("")
                            .addLore("§a" + I18n.as("itemscmd.copy"))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player p, ItemStack itemStack) {
                            if (type.isLeftClick() && !type.isShiftClick()) {
                                p.sendMessage(
                                        ColorAPI.adventure("§e" + typeName + "§7: §f" + valueStr)
                                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(valueStr))
                                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                                        ColorAPI.adventure("§c" + I18n.as("itemscmd.copy"))))
                                );
                            }
                        }
                    });
                }

                gui.openGUI(player);
                return true;
            }
            case "cmd" -> {
                if (args.length < 2) {
                    sender.sendMessage(I18n.as("info.cmd.usage"));
                    return false;
                }

                String commandName = args[1];
                PluginCommand pluginCommand = player.getServer().getPluginCommand(commandName);

                if (pluginCommand == null) {
                    Command unknownCommand = player.getServer().getCommandMap().getCommand(commandName);
                    if (unknownCommand == null) {
                        sender.sendMessage(I18n.as("info.cmd.non_plugin_command"));
                        return false;
                    } else {
                        sendCommandInfo(sender, unknownCommand);
                        return true;
                    }
                }

                sendCommandInfo(sender, pluginCommand);
                return true;
            }

            default -> {
                sender.sendMessage(I18n.as("commands.usage", usageMessage));
                return false;
            }
        }
    }

    public void sendCommandInfo(CommandSender sender, Command pluginCommand) {
        String commandLabel = pluginCommand.getLabel();
        String permission = pluginCommand.getPermission();
        String commandDescription = pluginCommand.getDescription();
        String aliases = String.join(", ", pluginCommand.getAliases());

        String firstLine = I18n.as("info.cmd.header_format", I18n.as("info.cmd.non_plugin_title"));
        String message = firstLine + "\n" +
                I18n.as("info.cmd.source", commandLabel) + "\n" +
                I18n.as("info.cmd.command_description", commandDescription) + "\n" +
                I18n.as("info.cmd.permission", permission != null ? permission : I18n.as("info.cmd.none")) + "\n" +
                I18n.as("info.cmd.aliases", aliases.isEmpty() ? I18n.as("info.cmd.none") : aliases);

        sender.sendMessage(message);
    }

    public void sendCommandInfo(CommandSender sender, PluginCommand pluginCommand) {
        String pluginName = pluginCommand.getPlugin().getName();
        String pluginVersion = pluginCommand.getPlugin().getPluginMeta().getVersion();
        String pluginDescription = pluginCommand.getPlugin().getPluginMeta().getDescription();
        List<String> pluginAuthors = pluginCommand.getPlugin().getPluginMeta().getAuthors();
        String permission = pluginCommand.getPermission();
        String commandDescription = pluginCommand.getDescription();
        String aliases = String.join(", ", pluginCommand.getAliases());
        String authorsDisplay = (pluginAuthors.size() > 3) ? (String.join(", ", pluginAuthors.subList(0, 3)) + " ...") : String.join(", ", pluginAuthors);

        if (pluginDescription != null && pluginDescription.length() > 40) {
            pluginDescription = pluginDescription.substring(0, 40) + " ...";
        }

        String firstLine = I18n.as("info.cmd.header_format", I18n.as("info.cmd.plugin_title"));
        String message = firstLine + "\n" +
                I18n.as("info.cmd.plugin_name", pluginName) + "\n" +
                I18n.as("info.cmd.plugin_version", pluginVersion) + "\n" +
                I18n.as("info.cmd.plugin_description", pluginDescription) + "\n" +
                I18n.as("info.cmd.plugin_authors", authorsDisplay) + "\n" +
                I18n.as("info.cmd.command_description", commandDescription) + "\n" +
                I18n.as("info.cmd.permission", permission != null ? permission : I18n.as("info.cmd.none")) + "\n" +
                I18n.as("info.cmd.aliases", aliases.isEmpty() ? I18n.as("info.cmd.none") : aliases);

        sender.sendMessage(message);
    }
}
