package com.mohistmc.youer.commands;

import com.mohistmc.tools.NumberUtil;
import com.mohistmc.tools.OSUtil;
import com.mohistmc.tools.StatsUtils;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.ai.AiChatHandler;
import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.feature.WorldBackup;
import com.mohistmc.youer.feature.entityclear.EntityClear;
import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.MemoryUtils;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;

public class YouerCommand extends Command {

    private static final String[] COMMAND_LIST = {
            "windows", "mods", "playermods", "reload", "version",
            "channels_incom", "channels_outgo", "speed",
            "heal", "help", "memoryfix", "showp",
            "backupworld"
    };

    private final List<String> params = Arrays.asList(COMMAND_LIST);

    public YouerCommand(String name) {
        super(name);
        this.description = "Youer related commands";
        this.usageMessage = "/youer [" + String.join("|", COMMAND_LIST) + "]";
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
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("heal") || args[0].equalsIgnoreCase("showp"))) {
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
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "mods" -> {
                // Not recommended for use in games, only test output
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.insidemods") + ServerAPI.modlists_Inside.size() + ") -> " + ServerAPI.modlists_Inside);
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.clientOnlymods") + ServerAPI.modlists_Client.size() + ") -> " + ServerAPI.modlists_Client);
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.serverOnlymods") + ServerAPI.modlists_Server.size() + ") -> " + ServerAPI.modlists_Server);
                sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.allMods") + ServerAPI.modlists_All.size() + ") -> " + ServerAPI.modlists_All);
            }
            case "playermods" -> {
                // Not recommended for use in games, only test output
                if (args.length == 1) {
                    sender.sendMessage(ChatColor.RED + "Usage: /youer playermods <playername>");
                    return false;
                }
                Player player = Bukkit.getPlayer(args[1]);
                if (player != null) {
                    sender.sendMessage(ChatColor.GREEN + String.valueOf(PlayerAPI.getModSize(player)) + " " + PlayerAPI.getModlist(player).toString());
                } else {
                    sender.sendMessage(ChatColor.RED + I18n.as("youercmd.playermods.playernotOnline", args[1]));
                }
            }
            case "" -> {
                MinecraftServer console = MinecraftServer.getServer();
                YouerConfig.init((File) console.options.valueOf("youer-settings"));
                AiChatHandler.configure();
                ((CraftServer) Bukkit.getServer()).initConfig();
                ((CraftServer) Bukkit.getServer()).loadCustomPermissions();
                SpigotConfig.init((File) console.options.valueOf("spigot-settings"));
                for (ServerLevel world : console.getAllLevels()) {
                    world.spigotConfig.init();
                }

                // 重启 EntityClear 调度器，使新的 enable/time 配置热生效
                EntityClear.stop();
                EntityClear.start();

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
                return true;
            }
            case "heal" -> {
                Player target;

                if (args.length == 1) {
                    if (sender instanceof Player player) {
                        target = player;
                    } else {
                        sender.sendMessage(ChatColor.RED + I18n.as("error.notplayer"));
                        return true;
                    }
                } else {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + I18n.as("youercmd.heal.playernotfound", args[1]));
                        return true;
                    }
                }

                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);
                target.setSaturation(5.0f);
                target.setFireTicks(0);

                if (sender.equals(target)) {
                    sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.heal.self"));
                } else {
                    sender.sendMessage(ChatColor.GREEN + I18n.as("youercmd.heal.other", target.getName()));
                    target.sendMessage(ChatColor.GREEN + I18n.as("youercmd.heal.byadmin"));
                }

                return true;
            }

            case "channels_incom" -> sender.sendMessage(ServerAPI.channels_Incoming().toString());
            case "channels_outgo" -> sender.sendMessage(ServerAPI.channels_Outgoing().toString());
            case "speed" -> {
                if (sender instanceof Player p) {
                    if (args.length == 2 && p.isOp()) {
                        Float speed = NumberUtil.toFloat(args[1]);
                        if (speed != null) {
                            if (p.isFlying()) {
                                if (speed >= 0.0f && speed < 11.0f) {
                                    p.setFlySpeed(speed / 10.0f);
                                    p.sendMessage(I18n.as("youercmd.playerflightspeedSet") + speed);
                                }
                            } else {
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
            case "memoryfix" -> {
                if (!OSUtil.getOS().isWindows()) {
                    sender.sendMessage(ChatColor.RED + I18n.as("youercmd.memoryfix.not.windows"));
                    return true;
                }
                String result = MemoryUtils.setProcessWorkingSetSize(50, 100);
                sender.sendMessage(ChatColor.GREEN + result);
                return true;
            }
            case "windows" -> {
                int playerAmount = Bukkit.getOnlinePlayers().size();
                int maxplayerAmount = Bukkit.getOfflinePlayers().length;
                boolean onlineMode = Bukkit.getOnlineMode();
                String bukkitVersion = Bukkit.getVersion();
                bukkitVersion = bukkitVersion.substring(bukkitVersion.indexOf("MC: ") + 4, bukkitVersion.length() - 1);
                String javaVersion = System.getProperty("java.version");
                String osName = System.getProperty("os.name");
                String osArch = System.getProperty("os.arch");
                String osVersion = System.getProperty("os.version");

                sender.sendMessage(ChatColor.GOLD + I18n.as("youercmd.windows.title"));

                sender.sendMessage(I18n.as("youercmd.windows.version") + bukkitVersion);
                sender.sendMessage(I18n.as("youercmd.windows.players") + playerAmount + "/" + maxplayerAmount);
                sender.sendMessage(I18n.as("youercmd.windows.onlineMode") + (onlineMode ? I18n.as("youercmd.windows.enabled") : I18n.as("youercmd.windows.disabled")));
                sender.sendMessage(I18n.as("youercmd.windows.javaVersion") + javaVersion);
                sender.sendMessage(I18n.as("youercmd.windows.jvmUptime") + getJVMUpTime());

                sender.sendMessage(I18n.as("youercmd.windows.memory") +
                        StatsUtils.BytesToMegaBytes(StatsUtils.freeMemory()) + "/" +
                        StatsUtils.BytesToMegaBytes(StatsUtils.totalMemory()) + "/" +
                        StatsUtils.BytesToMegaBytes(StatsUtils.maxMemory()) + " MB (" +
                        I18n.as("youercmd.windows.disk.free") + "/" +
                        I18n.as("youercmd.windows.disk.usable") + "/" +
                        I18n.as("youercmd.windows.disk.total") + ")");

                try {
                    sender.sendMessage(I18n.as("youercmd.windows.cpu") +
                            StatsUtils.LoadAverange() + "% " + I18n.as("youercmd.windows.cpu.loadavg") + ", " +
                            StatsUtils.getProcessCpuLoad() + "% " + I18n.as("youercmd.windows.cpu.process"));
                } catch (final Exception ignored) {
                    sender.sendMessage(I18n.as("youercmd.windows.cpu") + I18n.as("youercmd.windows.cpu.unavailable"));
                }

                sender.sendMessage(I18n.as("youercmd.windows.disk") +
                        StatsUtils.BytesToGigaBytes(StatsUtils.freeDisk()) + "/" +
                        StatsUtils.BytesToGigaBytes(StatsUtils.usableDisk()) + "/" +
                        StatsUtils.BytesToGigaBytes(StatsUtils.totalDisk()) + " GB (" +
                        I18n.as("youercmd.windows.disk.free") + "/" +
                        I18n.as("youercmd.windows.disk.usable") + "/" +
                        I18n.as("youercmd.windows.disk.total") + ")");

                sender.sendMessage(I18n.as("youercmd.windows.system") + osName + " " + osVersion + " (" + osArch + ")");

                sender.sendMessage(ChatColor.GOLD + I18n.as("youercmd.windows.separator"));
            }

            case "showp" -> {
                if (args.length == 2) {
                    Player p2 = Bukkit.getPlayer(args[1]);
                    if (p2 != null) {
                        float getMaxHealth = PlayerAPI.getNMSPlayer(p2).getMaxHealth();

                        sender.sendMessage(ChatColor.GOLD + I18n.as("youercmd.showp.title", p2.getName()));

                        sender.sendMessage(I18n.as("youercmd.showp.displayName") + p2.getDisplayName());
                        if (sender instanceof Player p1) {
                            PlayerAPI.sendMessageByCopy(p1, I18n.as("youercmd.showp.uuid"), String.valueOf(p2.getUniqueId()));
                            PlayerAPI.sendMessageByCopy(p1, I18n.as("youercmd.showp.ip"), p2.getAddress().getAddress().getHostAddress());
                        } else {
                            sender.sendMessage(I18n.as("youercmd.showp.uuid") + p2.getUniqueId());
                            sender.sendMessage(I18n.as("youercmd.showp.ip") + p2.getAddress().getAddress().getHostAddress());
                        }

                        sender.sendMessage(I18n.as("youercmd.showp.health") + p2.getHealth() + "/" + getMaxHealth);
                        sender.sendMessage(I18n.as("youercmd.showp.food") + p2.getFoodLevel());
                        sender.sendMessage(I18n.as("youercmd.showp.exp") + p2.getLevel());

                        org.bukkit.Location loc = p2.getLocation();
                        sender.sendMessage(I18n.as("youercmd.showp.location",
                                p2.getWorld().getName(),
                                String.valueOf(loc.getBlockX()),
                                String.valueOf(loc.getBlockY()),
                                String.valueOf(loc.getBlockZ())));

                        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(p2.getUniqueId());
                        if (offlinePlayer.getLastPlayed() > 0 && offlinePlayer.hasPlayedBefore()) {
                            org.bukkit.Location lastPlayedLocation = offlinePlayer.getLocation();
                            if (lastPlayedLocation != null) {
                                sender.sendMessage(I18n.as("youercmd.showp.lastLocation",
                                        lastPlayedLocation.getWorld().getName(),
                                        String.valueOf(lastPlayedLocation.getBlockX()),
                                        String.valueOf(lastPlayedLocation.getBlockY()),
                                        String.valueOf(lastPlayedLocation.getBlockZ())));
                            }
                        }

                        sender.sendMessage(I18n.as("youercmd.showp.gamemode") + p2.getGameMode());
                        sender.sendMessage(I18n.as("youercmd.showp.flySpeed") + p2.getFlySpeed());
                        sender.sendMessage(I18n.as("youercmd.showp.walkSpeed") + p2.getWalkSpeed());
                        sender.sendMessage(I18n.as("youercmd.showp.allowFlight") + p2.getAllowFlight());
                        sender.sendMessage(I18n.as("youercmd.showp.isFlying") + p2.isFlying());
                        sender.sendMessage(I18n.as("youercmd.showp.op") + p2.isOp());
                    } else {
                        sender.sendMessage(ChatColor.RED + I18n.as("youercmd.showp.offline"));
                    }
                }
            }
            case "backupworld" -> {
                if (!sender.isOp()) {
                    return true;
                }
                WorldBackup.backup(sender);
            }
            default -> {
                showHelp(sender);
                return true;
            }
        }

        return true;
    }

    public static String getJVMUpTime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        return String.format("%d %s %d %s %d %s %d %s",
                days, I18n.as("youercmd.time.days"),
                hours % 24, I18n.as("youercmd.time.hours"),
                minutes % 60, I18n.as("youercmd.time.minutes"),
                seconds % 60, I18n.as("youercmd.time.seconds"));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + I18n.as("youercmd.help.title"));
        sender.sendMessage(ChatColor.GREEN + "/youer mods" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.mods"));
        sender.sendMessage(ChatColor.GREEN + "/youer playermods <player>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.playermods"));
        sender.sendMessage(ChatColor.GREEN + "/youer reload" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.reload"));
        sender.sendMessage(ChatColor.GREEN + "/youer version" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.version"));
        sender.sendMessage(ChatColor.GREEN + "/youer heal [player]" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.heal"));
        sender.sendMessage(ChatColor.GREEN + "/youer speed <value>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.speed"));
        sender.sendMessage(ChatColor.GREEN + "/youer memoryfix" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.memoryfix"));
        sender.sendMessage(ChatColor.GREEN + "/youer channels_incom" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.channels_incom"));
        sender.sendMessage(ChatColor.GREEN + "/youer channels_outgo" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.channels_outgo"));
        sender.sendMessage(ChatColor.GREEN + "/youer help" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.help"));
    }
}
