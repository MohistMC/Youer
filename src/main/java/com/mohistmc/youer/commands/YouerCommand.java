package com.mohistmc.youer.commands;

import com.mohistmc.tools.NumberUtil;
import com.mohistmc.tools.OSUtil;
import com.mohistmc.tools.StringUtil;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.feature.PacketStatistics;
import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.MemoryUtils;
import com.mohistmc.youer.util.TimeUtils;
import com.mohistmc.youer.util.YouerThreadCost;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;

public class YouerCommand extends Command {

    private final List<String> params = Arrays.asList("mods", "playermods", "reload", "version", "channels_incom", "channels_outgo", "speed", "printthreadcost", "packetstats", "heal", "help", "cleardropitem", "memoryfix");

    public YouerCommand(String name) {
        super(name);
        this.description = "Youer related commands";
        this.usageMessage = "/youer [mods|playermods|reload|version|channels_incom|channels_outgo|speed|debug|packetstats|heal|help|cleardropitem|memoryfix]";
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
            } else if (args.length == 2 && args[0].equalsIgnoreCase("packetstats")) {
                return Stream.of("start", "stop", "status")
                        .filter(param -> param.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("heal")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("cleardropitem")) {
                return Bukkit.getWorldsByName().stream().toList();
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
            case "reload" -> {
                MinecraftServer console = MinecraftServer.getServer();
                YouerConfig.init((File) console.options.valueOf("youer-settings"));
                ((CraftServer) Bukkit.getServer()).initConfig();
                ((CraftServer) Bukkit.getServer()).loadCustomPermissions();
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
                return true;
            }
            case "debug" -> {
                Player player = Bukkit.getPlayer(args[1]);
                // 1. 渐变格式（支持带#和不带#）
                sender.sendMessage("<gradient:#FF0000:#0000FF>渐变文本</gradient>");
                sender.sendMessage("<gradient:FF0000:0000FF>渐变文本</gradient>");
                Bukkit.broadcastMessage("<gradient:#00FF00:#0000FF>这是一条绿到蓝渐变的公告</gradient>");
                player.setDisplayName("<#FF0000>玩家名称</#FF0000>");
                // 2. 固定颜色格式
                sender.sendMessage("<#FF0000>红色文本</#FF0000>");
                sender.sendMessage("<FF0000>红色文本</FF0000>");
                sender.sendMessage("<red>红色文本</red>");
                sender.sendMessage("<bold>粗体文本</bold>");

                // 3. 十六进制格式
                sender.sendMessage("&#FF0000红色文本\n<gradient:#00FF00:#0000FF>绿到蓝渐变</gradient>");

                // 4. 传统格式
                sender.sendMessage("&a绿色 &c红色 &l粗体");

                // 5. 混合格式
                sender.sendMessage(
                        "<gradient:#FF0000:#00FF00>渐变文本</gradient> " +
                                "<#0000FF>蓝色文本</#0000FF> " +
                                "&#FF0000红色文本 " +
                                "&a绿色文本"
                );
                return true;
            }
            case "packetstats" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("packetstats.usage"));
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "start" -> {
                        if (PacketStatistics.isCollecting()) {
                            sender.sendMessage(ChatColor.YELLOW + I18n.as("packetstats.already.running"));
                            return true;
                        }
                        PacketStatistics.startCollecting();
                        sender.sendMessage(ChatColor.GREEN + I18n.as("packetstats.started"));
                        sender.sendMessage(ChatColor.GRAY + I18n.as("packetstats.stop.to.view"));
                        return true;
                    }
                    case "stop" -> {
                        if (!PacketStatistics.isCollecting()) {
                            sender.sendMessage(ChatColor.YELLOW + I18n.as("packetstats.not.running"));
                            return true;
                        }

                        long stopTime = System.currentTimeMillis();
                        long durationMillis = stopTime - PacketStatistics.getStartTime();
                        long durationSeconds = durationMillis / 1000;
                        String durationString = TimeUtils.formatDuration(durationSeconds);

                        PacketStatistics.stopCollecting();
                        sender.sendMessage(ChatColor.GOLD + I18n.as("packetstats.report.title"));
                        sender.sendMessage(ChatColor.AQUA + I18n.as("packetstats.total.bytes", StringUtil.formatBytes(PacketStatistics.getTotalBytesSent())));
                        sender.sendMessage(ChatColor.AQUA + I18n.as("packetstats.total.packets", String.valueOf(PacketStatistics.getTotalPacketsSent())));
                        sender.sendMessage(ChatColor.AQUA + I18n.as("packetstats.transfer.rate", StringUtil.formatBytes(PacketStatistics.getBytesPerSecond())));
                        sender.sendMessage(ChatColor.AQUA + I18n.as("packetstats.packets.per.second", String.valueOf(PacketStatistics.getPacketsPerSecond())));
                        sender.sendMessage(ChatColor.AQUA + I18n.as("packetstats.duration", durationString));

                        Map<String, Long> bytesByPacketType = PacketStatistics.getBytesByPacketType();
                        if (!bytesByPacketType.isEmpty()) {
                            sender.sendMessage(ChatColor.GOLD + I18n.as("packetstats.by.type.title", String.valueOf(bytesByPacketType.size())));

                            List<Map.Entry<String, Long>> top10 = bytesByPacketType.entrySet().stream()
                                    .filter(entry -> entry.getValue() > 0 && PacketStatistics.getPacketsByPacketType().getOrDefault(entry.getKey(), 0L) > 0)
                                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                    .limit(10)
                                    .toList();

                            for (int i = 0; i < top10.size(); i++) {
                                Map.Entry<String, Long> entry = top10.get(i);
                                String packetType = entry.getKey();
                                long bytes = entry.getValue();
                                long packets = PacketStatistics.getPacketsByPacketType().getOrDefault(packetType, 0L);
                                long bytesPerSecond = PacketStatistics.getBytesPerSecondByPacketType(packetType);
                                long packetsPerSecond = PacketStatistics.getPacketsPerSecondByPacketType(packetType);

                                ChatColor rankColor = switch (i) {
                                    case 0 -> ChatColor.RED;
                                    case 1 -> ChatColor.GOLD;
                                    case 2 -> ChatColor.YELLOW;
                                    default -> ChatColor.WHITE;
                                };

                                sender.sendMessage(rankColor + String.format("%2d", i + 1) + ". " +
                                        ChatColor.GREEN + packetType + ChatColor.GRAY + ": " +
                                        ChatColor.AQUA + StringUtil.formatBytes(bytes) +
                                        ChatColor.GRAY + " (" + ChatColor.YELLOW + packets + ChatColor.DARK_GRAY + "p" + ChatColor.GRAY + ") " +
                                        ChatColor.DARK_AQUA + "| " +
                                        ChatColor.AQUA + StringUtil.formatBytes(bytesPerSecond) + "/s " +
                                        ChatColor.GRAY + "(" + ChatColor.YELLOW + packetsPerSecond + ChatColor.DARK_GRAY + "p" + ChatColor.GRAY + "/s)");
                            }

                            if (bytesByPacketType.size() > 10) {
                                sender.sendMessage(ChatColor.GRAY + I18n.as("packetstats.more.types", String.valueOf(bytesByPacketType.size() - 10)));
                            }

                            try {
                                java.nio.file.Path savePath = PacketStatistics.savePacketStatsToJson();
                                sender.sendMessage(ChatColor.GREEN + I18n.as("packetstats.saved", savePath.toAbsolutePath().toString()));
                            } catch (Exception e) {
                                sender.sendMessage(ChatColor.RED + I18n.as("packetstats.save.failed", e.getMessage()));
                            }
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + I18n.as("packetstats.no.data"));
                        }

                        return true;
                    }
                    case "status" -> {
                        if (PacketStatistics.isCollecting()) {
                            sender.sendMessage(ChatColor.GREEN + I18n.as("packetstats.status.running"));
                            sender.sendMessage(ChatColor.AQUA + I18n.as("packetstats.status.collected",
                                    StringUtil.formatBytes(PacketStatistics.getTotalBytesSent()) + ChatColor.AQUA + " / " +
                                            ChatColor.YELLOW + PacketStatistics.getTotalPacketsSent() + ChatColor.DARK_GRAY + "p"));
                        } else {
                            sender.sendMessage(ChatColor.RED + I18n.as("packetstats.status.not.running"));
                        }
                        return true;
                    }
                    default -> {
                        sender.sendMessage(ChatColor.RED + I18n.as("packetstats.usage"));
                        return false;
                    }
                }
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
            case "help" -> {
                showHelp(sender);
                return true;
            }

            case "channels_incom" -> sender.sendMessage(ServerAPI.channels_Incoming().toString());
            case "printthreadcost" -> YouerThreadCost.dumpThreadCpuTime(sender);
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
            case "cleardropitem" -> {
                if (args.length == 2) {
                    World world = Bukkit.getWorld(args[1]);
                    if (world == null) {
                        sender.sendMessage(ChatColor.RED + " World not found!");
                        return false;
                    } else {
                        AtomicInteger size = new AtomicInteger(0);
                        for (org.bukkit.entity.Entity entity : world.getEntities().stream().toList()) {
                            if (entity.getType() == EntityType.ITEM) {
                                ItemStack item = ((org.bukkit.entity.Item) entity).getItemStack();
                                entity.remove();
                                size.addAndGet(item.getAmount());
                            }
                        }
                        sender.sendMessage(I18n.as("worldcommands.world.cleardropitem", size.get(), world.getName()));
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /mohist cleardropitem <worldname>");
                    return false;
                }
            }
            case "memoryfix" -> {
                if (!OSUtil.getOS().isWindows()) { // 如果不是Windows系统
                    sender.sendMessage(ChatColor.RED + I18n.as("youercmd.memoryfix.not.windows"));
                    return true;
                }
                String result = MemoryUtils.setProcessWorkingSetSize(50, 100);
                sender.sendMessage(ChatColor.GREEN + result);
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
                return false;
            }
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + I18n.as("youercmd.help.title"));
        sender.sendMessage(ChatColor.GREEN + "/youer mods" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.mods"));
        sender.sendMessage(ChatColor.GREEN + "/youer playermods <player>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.playermods"));
        sender.sendMessage(ChatColor.GREEN + "/youer reload" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.reload"));
        sender.sendMessage(ChatColor.GREEN + "/youer version" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.version"));
        sender.sendMessage(ChatColor.GREEN + "/youer debug" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.debug"));
        sender.sendMessage(ChatColor.GREEN + "/youer packetstats <start|stop|status>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.packetstats"));
        sender.sendMessage(ChatColor.GREEN + "/youer heal [player]" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.heal"));
        sender.sendMessage(ChatColor.GREEN + "/youer speed <value>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.speed"));
        sender.sendMessage(ChatColor.GREEN + "/youer cleardropitem <world>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.cleardropitem"));
        sender.sendMessage(ChatColor.GREEN + "/youer memoryfix" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.memoryfix"));
        sender.sendMessage(ChatColor.GREEN + "/youer channels_incom" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.channels_incom"));
        sender.sendMessage(ChatColor.GREEN + "/youer channels_outgo" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.channels_outgo"));
        sender.sendMessage(ChatColor.GREEN + "/youer printthreadcost" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.printthreadcost"));
        sender.sendMessage(ChatColor.GREEN + "/youer help" + ChatColor.GRAY + " - " + ChatColor.YELLOW + I18n.as("youercmd.help.help"));
    }
}
