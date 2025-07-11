/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2024.
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

import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.MohistThreadCost;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;

public class YouerCommand extends Command {

    private final List<String> params = Arrays.asList("mods", "playermods", "reload", "version", "channels_incom", "channels_outgo", "speed", "printthreadcost");

    public YouerCommand(String name) {
        super(name);
        this.description = "Youer related commands";
        this.usageMessage = "/youer [mods|playermods|reload|version|channels_incom|channels_outgo|speed|debug]";
        this.setPermission("youer.command.youer");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if ((sender.isOp() || testPermission(sender))) {
            if (args.length == 1) {
                for (String param : params) {
                    if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                        list.add(param);
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("playermods")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
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
            sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
            return false;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "mods" -> {
                // Not recommended for use in games, only test output
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.insidemods") + ServerAPI.modlists_Inside.size() + ") -> " + ServerAPI.modlists_Inside);
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.clientOnlymods")+ ServerAPI.modlists_Client.size() + ") -> " + ServerAPI.modlists_Client);
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.serverOnlymods") + ServerAPI.modlists_Server.size() + ") -> " + ServerAPI.modlists_Server);
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.allMods") + ServerAPI.modlists_All.size() + ") -> " + ServerAPI.modlists_All);
            }
            case "playermods" -> {
                // Not recommended for use in games, only test output
                if (args.length == 1) {
                    sender.sendMessage(ChatColor.RED + "Usage: /mohist playermods <playername>");
                    return false;
                }
                Player player = Bukkit.getPlayer(args[1]);
                if (player != null) {
                    sender.sendMessage(ChatColor.GREEN + String.valueOf(PlayerAPI.getModSize(player)) + " " + PlayerAPI.getModlist(player).toString());
                } else {
                    sender.sendMessage(ChatColor.RED + I18n.as("youercmd.playermods.playernotOnline", args[1]));
                }
            }
            case "reload" -> {
                MinecraftServer console = MinecraftServer.getServer();
                YouerConfig.init((File) console.options.valueOf("youer-settings"));
                ((CraftServer)Bukkit.getServer()).initConfig();
                ((CraftServer)Bukkit.getServer()).loadCustomPermissions();
                SpigotConfig.init((File) console.options.valueOf("spigot-settings"));
                for (ServerLevel world : console.getAllLevels()) {
                    world.spigotConfig.init();
                }

                console.server.reloadCount++;
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.reload.complete"));
                return true;
            }
            case "version" -> {
                sender.sendMessage("Youer: " + Youer.versionInfo.youer());
                sender.sendMessage("NeoForge: " + Youer.versionInfo.neoforge());
                sender.sendMessage("Bukkit: " + Youer.versionInfo.bukkit());
                sender.sendMessage("CraftBukkit: " + Youer.versionInfo.craftbukkit());
                sender.sendMessage("Spigot: " + Youer.versionInfo.spigot());
            }
            case "debug" -> {
                var registry = ServerAPI.getNMSServer().registryAccess().registryOrThrow(Registries.WORLD_PRESET);
                for (var dd : registry) {
                    sender.sendMessage("Youer: " + registry.getKey(dd));
                }
            }
            case "channels_incom" -> sender.sendMessage(ServerAPI.channels_Incoming().toString());
            case "printthreadcost" -> MohistThreadCost.dumpThreadCpuTime();
            case "channels_outgo" -> sender.sendMessage(ServerAPI.channels_Outgoing().toString());
            case "speed" -> {
                if (sender instanceof Player p) {
                    if (args.length == 2 && p.isOp()) {
                        if (this.isFloat(args[1])) {
                            if (p.isFlying()) {
                                float speed = Float.parseFloat(args[1]);
                                if (speed >= 0.0f && speed < 11.0f) {
                                    p.setFlySpeed(speed / 10.0f);
                                    p.sendMessage(I18n.as("youercmd.playerflightspeedSet") + speed);
                                }
                            } else {
                                float speed = Float.parseFloat(args[1]);
                                if (speed >= 0.0f && speed < 11.0f) {
                                    p.setWalkSpeed(speed / 10.0f);
                                    p.sendMessage(I18n.as("youercmd.playerwalkspeedset") + speed);
                                }
                            }
                        }
                        if (args[0].equalsIgnoreCase("reset")) {
                            p.setFlySpeed(0.1f);
                            p.setWalkSpeed(0.2f);
                            p.sendMessage(I18n.as("youercmd.flightAndWalkspeedRestore"));
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + I18n.as("error.notplayer"));
                }
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
                return false;
            }
        }


        return true;
    }

    private boolean isFloat(String input) {
        try {
            Float.parseFloat(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
