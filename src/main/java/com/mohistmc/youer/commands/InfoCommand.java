package com.mohistmc.youer.commands;

import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul by MohistMC
 * @date 2025/8/31 20:00:00
 */
public class InfoCommand extends Command {

    private final List<String> params = List.of("item", "block", "entity", "cmd");

    public InfoCommand(String name) {
        super(name);
        this.description = "Youer infos commands";
        this.usageMessage = "/infos [item|block|entity|cmd]";
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
            sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
            return false;
        }


        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + I18n.as("error.notplayer"));
            return false;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "item" -> {
                if (itemStack == null || itemStack.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                ItemsCommand.info(player);
                return true;
            }
            case "block" -> {
                Block block = player.getTargetBlockExact(5);
                if (block == null) {
                    sender.sendMessage(ChatColor.RED + I18n.as("info.block.not_found"));
                    return false;
                } else {
                    PlayerAPI.sendMessageByCopy(player, ChatColor.GREEN + I18n.as("info.block.type") + ChatColor.YELLOW, block.getType().name());
                    PlayerAPI.sendMessageByCopy(player, ChatColor.GREEN + I18n.as("info.block.key") + ChatColor.YELLOW, block.getType().getKey().asString());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.data") + ChatColor.YELLOW + block.getBlockData().getAsString());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.pos") + ChatColor.YELLOW + block.getX() + ", " + block.getY() + ", " + block.getZ());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.world") + ChatColor.YELLOW + block.getWorld().getName());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.light") + ChatColor.YELLOW + block.getLightLevel());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.biome") + ChatColor.YELLOW + block.getBiome().name());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.hardness") + ChatColor.YELLOW + block.getType().getHardness());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.resistance") + ChatColor.YELLOW + block.getType().getBlastResistance());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.slipperiness") + ChatColor.YELLOW + block.getType().getSlipperiness());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.replaceable") + ChatColor.YELLOW + block.isReplaceable());
                    sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.requires_tool") + ChatColor.YELLOW + block.getBlockData().requiresCorrectToolForDrops());

                    if (block.getType().isSolid()) {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.solid") + ChatColor.YELLOW + I18n.as("info.yes"));
                    } else {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.solid") + ChatColor.YELLOW + I18n.as("info.no"));
                    }

                    if (block.getType().isBurnable()) {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.burnable") + ChatColor.YELLOW + I18n.as("info.yes"));
                    } else {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.burnable") + ChatColor.YELLOW + I18n.as("info.no"));
                    }

                    if (block.getType().isInteractable()) {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.interactable") + ChatColor.YELLOW + I18n.as("info.yes"));
                    } else {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.interactable") + ChatColor.YELLOW + I18n.as("info.no"));
                    }

                    if (block.getType().hasGravity()) {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.gravity") + ChatColor.YELLOW + I18n.as("info.yes"));
                    } else {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.gravity") + ChatColor.YELLOW + I18n.as("info.no"));
                    }

                    if (block.getState() != null) {
                        sender.sendMessage(ChatColor.GREEN + I18n.as("info.block.state") + ChatColor.YELLOW + block.getState().getClass().getSimpleName());
                    }
                }
                return true;
            }
            case "entity" -> {
                Entity entity = player.getTargetEntity(5, true);
                if (entity == null) {
                    sender.sendMessage(ChatColor.RED + I18n.as("info.entity.not_found"));
                    return false;
                } else {
                    PlayerAPI.sendMessageByCopy(player, ChatColor.GOLD + I18n.as("info.entity.type") + ChatColor.YELLOW, entity.getType().name());
                    PlayerAPI.sendMessageByCopy(player, ChatColor.GOLD + I18n.as("info.entity.key") + ChatColor.YELLOW, entity.getType().getKey().asString());
                    PlayerAPI.sendMessageByCopy(player, ChatColor.GOLD + I18n.as("info.entity.uuid") + ChatColor.YELLOW, entity.getUniqueId().toString());
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.pos") + ChatColor.YELLOW +
                            String.format("%.2f, %.2f, %.2f", entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ()));
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.world") + ChatColor.YELLOW + entity.getWorld().getName());
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.health") + ChatColor.YELLOW +
                            (entity instanceof org.bukkit.entity.Damageable ? ((org.bukkit.entity.Damageable) entity).getHealth() : "N/A"));
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.onground") + ChatColor.YELLOW + entity.isOnGround());
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.burning") + ChatColor.YELLOW + entity.isVisualFire());
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.ticks") + ChatColor.YELLOW + entity.getTicksLived() + " ticks");
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.frozen") + ChatColor.YELLOW + entity.isFrozen());
                    sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.gravity") + ChatColor.YELLOW + entity.hasGravity());

                    if (entity.getCustomName() != null) {
                        sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.custom_name") + ChatColor.YELLOW + entity.getCustomName());
                    }

                    if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
                        sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.max_health") + ChatColor.YELLOW + livingEntity.getMaxHealth());
                        sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.ai") + ChatColor.YELLOW + livingEntity.hasAI());
                        sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.sleeping") + ChatColor.YELLOW + livingEntity.isSleeping());

                        if (livingEntity instanceof Player targetPlayer) {
                            sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.player_info"));
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.gamemode") + ChatColor.YELLOW + targetPlayer.getGameMode().name());
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.level") + ChatColor.YELLOW + targetPlayer.getLevel());
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.exp") + ChatColor.YELLOW + targetPlayer.getTotalExperience());
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.food") + ChatColor.YELLOW + targetPlayer.getFoodLevel());
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.saturation") + ChatColor.YELLOW + targetPlayer.getSaturation());
                        }

                        sender.sendMessage(ChatColor.GOLD + I18n.as("info.entity.equipment"));
                        if (livingEntity.getEquipment() != null) {
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.main_hand") + ChatColor.YELLOW +
                                    (livingEntity.getEquipment().getItemInMainHand().getType() != Material.AIR ?
                                            livingEntity.getEquipment().getItemInMainHand().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.off_hand") + ChatColor.YELLOW +
                                    (livingEntity.getEquipment().getItemInOffHand().getType() != Material.AIR ?
                                            livingEntity.getEquipment().getItemInOffHand().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.helmet") + ChatColor.YELLOW +
                                    (livingEntity.getEquipment().getHelmet() != null ?
                                            livingEntity.getEquipment().getHelmet().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.chestplate") + ChatColor.YELLOW +
                                    (livingEntity.getEquipment().getChestplate() != null ?
                                            livingEntity.getEquipment().getChestplate().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.leggings") + ChatColor.YELLOW +
                                    (livingEntity.getEquipment().getLeggings() != null ?
                                            livingEntity.getEquipment().getLeggings().getType().name() : I18n.as("info.entity.empty")));
                            sender.sendMessage(ChatColor.GOLD + "  " + I18n.as("info.entity.boots") + ChatColor.YELLOW +
                                    (livingEntity.getEquipment().getBoots() != null ?
                                            livingEntity.getEquipment().getBoots().getType().name() : I18n.as("info.entity.empty")));
                        }
                    }
                }
                return true;
            }
            case "cmd" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("info.cmd.usage"));
                    return false;
                }

                String commandName = args[1];
                PluginCommand pluginCommand = player.getServer().getPluginCommand(commandName);

                if (pluginCommand == null) {
                    Command unknownCommand = player.getServer().getCommandMap().getCommand(commandName);
                    if (unknownCommand == null) {
                        sender.sendMessage(ChatColor.YELLOW + I18n.as("info.cmd.non_plugin_command"));
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
                sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
                return false;
            }
        }
    }

    public void sendCommandInfo(CommandSender sender, Command pluginCommand) {
        String commandLabel = pluginCommand.getLabel();
        String permission = pluginCommand.getPermission();
        String commandDescription = pluginCommand.getDescription();
        String aliases = String.join(", ", pluginCommand.getAliases());

        String firstLine = "&6&m----------------&r&6 " + I18n.as("info.cmd.non_plugin_title") + " &6&m----------------";
        String message = firstLine + "\n" +
                "&e" + I18n.as("info.cmd.source") + " &f" + commandLabel + "\n" +
                "&e" + I18n.as("info.cmd.command_description") + " &f" + commandDescription + "\n" +
                "&e" + I18n.as("info.cmd.permission") + " &f" + (permission != null ? permission : I18n.as("info.cmd.none")) + "\n" +
                "&e" + I18n.as("info.cmd.aliases") + " &f" + (aliases.isEmpty() ? I18n.as("info.cmd.none") : aliases);

        sender.sendMessage(message);
    }

    public void sendCommandInfo(CommandSender sender, PluginCommand pluginCommand) {
        String pluginName = pluginCommand.getPlugin().getName();
        String pluginVersion = pluginCommand.getPlugin().getDescription().getVersion();
        String pluginDescription = pluginCommand.getPlugin().getDescription().getDescription();
        List<String> pluginAuthors = pluginCommand.getPlugin().getDescription().getAuthors();
        String permission = pluginCommand.getPermission();
        String commandDescription = pluginCommand.getDescription();
        String aliases = String.join(", ", pluginCommand.getAliases());
        String authorsDisplay = (pluginAuthors.size() > 3) ? (String.join(", ", pluginAuthors.subList(0, 3)) + " ...") : String.join(", ", pluginAuthors);

        if (pluginDescription != null && pluginDescription.length() > 40) {
            pluginDescription = pluginDescription.substring(0, 40) + " ...";
        }

        String firstLine = "&6&m----------------&r&6 " + I18n.as("info.cmd.plugin_title") + " &6&m----------------";
        String message = firstLine + "\n" +
                "&e" + I18n.as("info.cmd.plugin_name") + " &f" + pluginName + "\n" +
                "&e" + I18n.as("info.cmd.plugin_version") + " &f" + pluginVersion + "\n" +
                "&e" + I18n.as("info.cmd.plugin_description") + " &f" + pluginDescription + "\n" +
                "&e" + I18n.as("info.cmd.plugin_authors") + " &f" + authorsDisplay + "\n" +
                "&e" + I18n.as("info.cmd.command_description") + " &f" + commandDescription + "\n" +
                "&e" + I18n.as("info.cmd.permission") + " &f" + (permission != null ? permission : I18n.as("info.cmd.none")) + "\n" +
                "&e" + I18n.as("info.cmd.aliases") + " &f" + (aliases.isEmpty() ? I18n.as("info.cmd.none") : aliases);

        sender.sendMessage(message);
    }
}
