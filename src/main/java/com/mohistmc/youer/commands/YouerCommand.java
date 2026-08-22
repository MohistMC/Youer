package com.mohistmc.youer.commands;

import com.mohistmc.tools.NumberUtil;
import com.mohistmc.tools.OSUtil;
import com.mohistmc.tools.StatsUtils;
import com.mohistmc.tools.StringUtil;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.PlayerAPI;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.feature.PacketStatistics;
import com.mohistmc.youer.feature.WorldBackup;
import org.purpurmc.purpur.task.PacketBarTask;
import com.mohistmc.youer.feature.db.DatabaseMigration;
import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.MemoryUtils;
import com.mohistmc.youer.util.TimeUtils;
import com.mohistmc.youer.util.YouerThreadCost;
import com.mohistmc.youer.util.YouerVersion;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.common.NeoForgeVersion;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;

public class YouerCommand extends Command {

    private static final String[] COMMAND_LIST = {
            "info", "mods", "playermods", "reload", "version",
            "channels_incom", "channels_outgo", "modchannels", "speed", "printthreadcost",
            "packetstats", "heal", "help", "cleardropitem", "memoryfix", "showp",
            "backupworld", "migratedb"
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
            } else if (args.length == 2 && args[0].equalsIgnoreCase("packetstats")) {
                return Stream.of("start", "stop", "status")
                        .filter(param -> param.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("heal") || args[0].equalsIgnoreCase("showp"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("cleardropitem")) {
                return Bukkit.getWorlds().stream().map(WorldInfo::getName).toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("migratedb")) {
                List<String> mods = new ArrayList<>(DatabaseMigration.getModuleNames());
                mods.add("all");
                return mods.stream()
                        .filter(m -> m.toLowerCase().startsWith(args[1].toLowerCase()))
                        .sorted()
                        .toList();
            } else if (args.length == 3 && args[0].equalsIgnoreCase("migratedb")) {
                return Stream.of("sqlite", "mysql")
                        .filter(t -> t.startsWith(args[2].toLowerCase()))
                        .toList();
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
                sender.sendMessage(I18n.as("youercmd.insidemods", ServerAPI.modlists_Inside.size(), ServerAPI.modlists_Inside));
                sender.sendMessage(I18n.as("youercmd.clientOnlymods", ServerAPI.modlists_Client.size(), ServerAPI.modlists_Client));
                sender.sendMessage(I18n.as("youercmd.serverOnlymods", ServerAPI.modlists_Server.size(), ServerAPI.modlists_Server));
                sender.sendMessage(I18n.as("youercmd.allMods", ServerAPI.modlists_All.size(), ServerAPI.modlists_All));
            }
            case "playermods" -> {
                // Not recommended for use in games, only test output
                if (args.length == 1) {
                    sender.sendMessage(I18n.as("youercmd.usage.playermods"));
                    return false;
                }
                Player player = Bukkit.getPlayer(args[1]);
                if (player != null) {
                    sender.sendMessage(I18n.as("youercmd.playermods.info", PlayerAPI.getModSize(player), PlayerAPI.getModlist(player).toString()));
                } else {
                    sender.sendMessage(I18n.as("youercmd.playermods.playernotOnline", args[1]));
                }
            }
            case "reload" -> {
                MinecraftServer console = MinecraftServer.getServer();
                YouerConfig.init((File) console.options.valueOf("youer-settings"));
                ((CraftServer) Bukkit.getServer()).initConfig();
                ((CraftServer) Bukkit.getServer()).loadCustomPermissions();
                SpigotConfig.init((File) console.options.valueOf("spigot-settings"));
                for (ServerLevel world : console.getAllLevels()) {
                    world.spigotConfig.init(); // TODO
                }

                console.server.reloadCount++;
                sender.sendMessage(I18n.as("youercmd.reload.complete"));
                return true;
            }
            case "version" -> {
                sender.sendMessage("Youer: " + YouerVersion.getVersion());
                sender.sendMessage("NeoForge: " + NeoForgeVersion.getVersion());
                sender.sendMessage("Paper: " + YouerVersion.getPaperVersion());
                sender.sendMessage("Purpur: " + YouerVersion.getPurpurVersion());
                return true;
            }
            case "packetstats" -> {
                if (args.length < 2) {
                    sender.sendMessage(I18n.as("packetstats.usage"));
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "start" -> {
                        if (PacketStatistics.isCollecting()) {
                            sender.sendMessage(I18n.as("packetstats.already.running"));
                            return true;
                        }
                        PacketStatistics.startCollecting();
                        sender.sendMessage(I18n.as("packetstats.started"));
                        sender.sendMessage(I18n.as("packetstats.stop.to.view"));
                        return true;
                    }
                    case "stop" -> {
                        if (!PacketStatistics.isCollecting()) {
                            sender.sendMessage(I18n.as("packetstats.not.running"));
                            return true;
                        }

                        long stopTime = System.currentTimeMillis();
                        long durationMillis = stopTime - PacketStatistics.getStartTime();
                        long durationSeconds = durationMillis / 1000;
                        String durationString = TimeUtils.formatDuration(durationSeconds);

                        PacketStatistics.stopCollecting();

                        // Stop the packet bar too — it would otherwise show stale zeros
                        PacketBarTask.instance().stop();

                        sender.sendMessage(I18n.as("packetstats.report.title"));
                        sender.sendMessage(I18n.as("packetstats.total.bytes", StringUtil.formatBytes(PacketStatistics.getTotalBytesSent())));
                        sender.sendMessage(I18n.as("packetstats.total.packets", String.valueOf(PacketStatistics.getTotalPacketsSent())));
                        sender.sendMessage(I18n.as("packetstats.transfer.rate", StringUtil.formatBytes(PacketStatistics.getBytesPerSecond())));
                        sender.sendMessage(I18n.as("packetstats.packets.per.second", String.valueOf(PacketStatistics.getPacketsPerSecond())));
                        sender.sendMessage(I18n.as("packetstats.duration", durationString));

                        Map<String, Long> bytesByPacketType = PacketStatistics.getBytesByPacketType();
                        if (!bytesByPacketType.isEmpty()) {
                            sender.sendMessage(I18n.as("packetstats.by.type.title", String.valueOf(bytesByPacketType.size())));

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

                                String rankColor = switch (i) {
                                    case 0 -> "#朱砂";
                                    case 1 -> "#金色";
                                    case 2 -> "#鹅黄";
                                    default -> "#white";
                                };

                                sender.sendMessage(rankColor + String.format("%2d", i + 1) + ". " +
                                        "#碧色" + packetType + "#灰色: " +
                                        "#aqua" + StringUtil.formatBytes(bytes) +
                                        "#灰色 (" + "#鹅黄" + packets + "#dark_gray" + "p" + "#灰色) " +
                                        "#dark_aqua" + "| " +
                                        "#aqua" + StringUtil.formatBytes(bytesPerSecond) + "/s " +
                                        "#灰色(" + "#鹅黄" + packetsPerSecond + "#dark_gray" + "p" + "#灰色/s)");
                            }

                            if (bytesByPacketType.size() > 10) {
                                sender.sendMessage(I18n.as("packetstats.more.types", String.valueOf(bytesByPacketType.size() - 10)));
                            }

                            try {
                                java.nio.file.Path savePath = PacketStatistics.savePacketStatsToJson();
                                sender.sendMessage(I18n.as("packetstats.saved", savePath.toAbsolutePath().toString()));
                            } catch (Exception e) {
                                sender.sendMessage(I18n.as("packetstats.save.failed", e.getMessage()));
                            }
                        } else {
                            sender.sendMessage(I18n.as("packetstats.no.data"));
                        }

                        return true;
                    }
                    case "status" -> {
                        if (PacketStatistics.isCollecting()) {
                            sender.sendMessage(I18n.as("packetstats.status.running"));
                            sender.sendMessage(I18n.as("packetstats.status.collected",
                                    StringUtil.formatBytes(PacketStatistics.getTotalBytesSent()),
                                    PacketStatistics.getTotalPacketsSent()));
                        } else {
                            sender.sendMessage(I18n.as("packetstats.status.not.running"));
                        }
                        return true;
                    }
                    default -> {
                        sender.sendMessage(I18n.as("packetstats.usage"));
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
                        sender.sendMessage(I18n.as("error.notplayer"));
                        return true;
                    }
                } else {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(I18n.as("youercmd.heal.playernotfound", args[1]));
                        return true;
                    }
                }

                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);
                target.setSaturation(5.0f);
                target.setFireTicks(0);

                if (sender.equals(target)) {
                    sender.sendMessage(I18n.as("youercmd.heal.self"));
                } else {
                    sender.sendMessage(I18n.as("youercmd.heal.other", target.getName()));
                    target.sendMessage(I18n.as("youercmd.heal.byadmin"));
                }

                return true;
            }

            case "channels_incom" -> {
                listPluginChannels(sender, true);
                return true;
            }
            case "channels_outgo" -> {
                listPluginChannels(sender, false);
                return true;
            }
            case "modchannels" -> {
                // List every custom payload channel registered by mods through NeoForge.
                sender.sendMessage(I18n.as("youercmd.modchannels.title"));
                boolean any = false;
                for (ConnectionProtocol protocol : NetworkRegistry.PAYLOAD_REGISTRATIONS.keySet()) {
                    var registrations = NetworkRegistry.PAYLOAD_REGISTRATIONS.get(protocol);
                    if (registrations.isEmpty()) {
                        continue;
                    }
                    any = true;
                    sender.sendMessage(I18n.as("youercmd.modchannels.protocol", protocol.toString(), registrations.size()));
                    for (var entry : registrations.entrySet()) {
                        var reg = entry.getValue();
                        String flow = reg.flow()
                                .map(f -> f == PacketFlow.SERVERBOUND ? "UPSTREAM" : "DOWNSTREAM")
                                .orElse("BIDIRECTIONAL");
                        sender.sendMessage(I18n.as("youercmd.modchannels.entry", entry.getKey().toString(), flow, reg.version()));
                    }
                }
                if (!any) {
                    sender.sendMessage(I18n.as("youercmd.modchannels.empty"));
                }
                return true;
            }
            case "speed" -> {
                if (sender instanceof Player p) {
                    if (args.length == 2 && p.isOp()) {
                        Float speed = NumberUtil.toFloat(args[1]);
                        if (speed != null) {
                            if (p.isFlying()) {
                                if (speed >= 0.0f && speed < 11.0f) {
                                    p.setFlySpeed(speed / 10.0f);
                                    p.sendMessage(I18n.as("youercmd.playerflightspeedSet", speed));
                                }
                            } else {
                                if (speed >= 0.0f && speed < 11.0f) {
                                    p.setWalkSpeed(speed / 10.0f);
                                    p.sendMessage(I18n.as("youercmd.playerwalkspeedset", speed));
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
                    sender.sendMessage(I18n.as("error.notplayer"));
                }
            }
            case "cleardropitem" -> {
                if (args.length == 2) {
                    World world = Bukkit.getWorld(args[1]);
                    if (world == null) {
                        sender.sendMessage(I18n.as("youercmd.world.notfound"));
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
                    sender.sendMessage(I18n.as("youercmd.usage.cleardropitem"));
                    return false;
                }
            }
            case "memoryfix" -> {
                if (!OSUtil.getOS().isWindows()) {
                    sender.sendMessage(I18n.as("youercmd.memoryfix.not.windows"));
                    return true;
                }
                String result = MemoryUtils.setProcessWorkingSetSize(50, 100);
                sender.sendMessage(I18n.as("youercmd.memoryfix.result", result));
                return true;
            }
            case "info" -> {
                int pid = (int) ProcessHandle.current().pid();
                int playerAmount = Bukkit.getOnlinePlayers().size();
                int maxplayerAmount = Bukkit.getMaxPlayers();
                boolean onlineMode = Bukkit.getOnlineMode();
                String bukkitVersion = Bukkit.getVersion();
                bukkitVersion = bukkitVersion.substring(bukkitVersion.indexOf("MC: ") + 4, bukkitVersion.length() - 1);
                String javaVersion = System.getProperty("java.version");
                String osName = System.getProperty("os.name");
                String osArch = System.getProperty("os.arch");
                String osVersion = System.getProperty("os.version");

                sender.sendMessage(I18n.as("youercmd.info.title"));

                sender.sendMessage(I18n.as("youercmd.info.pid", pid));
                sender.sendMessage(I18n.as("youercmd.info.version", bukkitVersion));
                sender.sendMessage(I18n.as("youercmd.info.players", playerAmount, maxplayerAmount));
                sender.sendMessage(I18n.as("youercmd.info.onlineMode", onlineMode ? I18n.as("youercmd.info.enabled") : I18n.as("youercmd.info.disabled")));
                sender.sendMessage(I18n.as("youercmd.info.javaVersion", javaVersion));
                sender.sendMessage(I18n.as("youercmd.info.jvmUptime", getJVMUpTime()));

                sender.sendMessage(I18n.as("youercmd.info.memory",
                        StatsUtils.BytesToMegaBytes(StatsUtils.freeMemory()),
                        StatsUtils.BytesToMegaBytes(StatsUtils.totalMemory()),
                        StatsUtils.BytesToMegaBytes(StatsUtils.maxMemory()),
                        I18n.as("youercmd.info.disk.free"),
                        I18n.as("youercmd.info.disk.usable"),
                        I18n.as("youercmd.info.disk.total")));

                try {
                    sender.sendMessage(I18n.as("youercmd.info.cpu",
                            StatsUtils.LoadAverange(),
                            I18n.as("youercmd.info.cpu.loadavg"),
                            StatsUtils.getProcessCpuLoad(),
                            I18n.as("youercmd.info.cpu.process")));
                } catch (final Exception ignored) {
                    sender.sendMessage(I18n.as("youercmd.info.cpu.unavailable_msg"));
                }

                sender.sendMessage(I18n.as("youercmd.info.disk",
                        StatsUtils.BytesToGigaBytes(StatsUtils.freeDisk()),
                        StatsUtils.BytesToGigaBytes(StatsUtils.usableDisk()),
                        StatsUtils.BytesToGigaBytes(StatsUtils.totalDisk()),
                        I18n.as("youercmd.info.disk.free"),
                        I18n.as("youercmd.info.disk.usable"),
                        I18n.as("youercmd.info.disk.total")));

                sender.sendMessage(I18n.as("youercmd.info.system", osName, osVersion, osArch));

                sender.sendMessage(I18n.as("youercmd.info.separator"));
            }

            case "showp" -> {
                if (args.length == 2) {
                    Player p2 = Bukkit.getPlayer(args[1]);
                    if (p2 != null) {
                        float getMaxHealth = PlayerAPI.getNMSPlayer(p2).getMaxHealth();

                        sender.sendMessage(I18n.as("youercmd.showp.title", p2.getName()));

                        sender.sendMessage(I18n.as("youercmd.showp.displayName", p2.getDisplayName()));
                        if (sender instanceof Player p1) {
                            PlayerAPI.sendMessageByCopy(p1, I18n.as("youercmd.showp.uuid"), String.valueOf(p2.getUniqueId()));
                            PlayerAPI.sendMessageByCopy(p1, I18n.as("youercmd.showp.ip"), p2.getAddress().getAddress().getHostAddress());
                        } else {
                            sender.sendMessage(I18n.as("youercmd.showp.uuid", p2.getUniqueId()));
                            sender.sendMessage(I18n.as("youercmd.showp.ip", p2.getAddress().getAddress().getHostAddress()));
                        }

                        sender.sendMessage(I18n.as("youercmd.showp.health", p2.getHealth(), getMaxHealth));
                        sender.sendMessage(I18n.as("youercmd.showp.food", p2.getFoodLevel()));
                        sender.sendMessage(I18n.as("youercmd.showp.exp", p2.getLevel()));

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

                        sender.sendMessage(I18n.as("youercmd.showp.gamemode", p2.getGameMode()));
                        sender.sendMessage(I18n.as("youercmd.showp.flySpeed", p2.getFlySpeed()));
                        sender.sendMessage(I18n.as("youercmd.showp.walkSpeed", p2.getWalkSpeed()));
                        sender.sendMessage(I18n.as("youercmd.showp.allowFlight", p2.getAllowFlight()));
                        sender.sendMessage(I18n.as("youercmd.showp.isFlying", p2.isFlying()));
                        sender.sendMessage(I18n.as("youercmd.showp.op", p2.isOp()));
                    } else {
                        sender.sendMessage(I18n.as("youercmd.showp.offline"));
                    }
                }
            }
            case "backupworld" -> {
                if (!sender.isOp()) {
                    return true;
                }
                WorldBackup.backup(sender);
            }
            case "migratedb" -> {
                if (!sender.isOp()) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(I18n.as("migratedb.usage"));
                    return false;
                }
                String module = args[1].toLowerCase(Locale.ENGLISH);
                String target = args[2].toLowerCase(Locale.ENGLISH);

                if (!target.equals("sqlite") && !target.equals("mysql")) {
                    sender.sendMessage(I18n.as("migratedb.invalid_target"));
                    return false;
                }

                new Thread(() -> {
                    if ("all".equals(module)) {
                        migrateAll(sender, target);
                    } else {
                        sender.sendMessage(I18n.as("migratedb.starting",
                                DatabaseMigration.getDisplayName(module), target));
                        DatabaseMigration.MigrationResult result = DatabaseMigration.migrate(module, target);
                        if (result.success()) {
                            sender.sendMessage(I18n.as("migratedb.success",
                                    DatabaseMigration.getDisplayName(module), result.rows()));
                        } else {
                            sender.sendMessage(I18n.as("migratedb.fail",
                                    DatabaseMigration.getDisplayName(module), result.message()));
                        }
                    }
                }, "DB-Migration-" + module).start();
            }
            default -> {
                showHelp(sender);
                return true;
            }
        }

        return true;
    }

    private void listPluginChannels(CommandSender sender, boolean incoming) {
        var messenger = Bukkit.getMessenger();
        var byChannel = new TreeMap<String, TreeSet<String>>();
        for (var plugin : Bukkit.getPluginManager().getPlugins()) {
            var channels = incoming ? messenger.getIncomingChannels(plugin) : messenger.getOutgoingChannels(plugin);
            for (var ch : channels) {
                byChannel.computeIfAbsent(ch, k -> new TreeSet<>()).add(plugin.getName());
            }
        }
        sender.sendMessage(I18n.as(incoming ? "youercmd.channels_incom.title" : "youercmd.channels_outgo.title"));
        if (byChannel.isEmpty()) {
            sender.sendMessage(I18n.as("youercmd.channels.empty"));
            return;
        }
        for (var entry : byChannel.entrySet()) {
            sender.sendMessage(I18n.as("youercmd.channels.entry", entry.getKey(), String.join(", ", entry.getValue())));
        }
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
        sender.sendMessage(I18n.as("youercmd.help.title"));
        sender.sendMessage(I18n.as("youercmd.help.mods"));
        sender.sendMessage(I18n.as("youercmd.help.playermods"));
        sender.sendMessage(I18n.as("youercmd.help.reload"));
        sender.sendMessage(I18n.as("youercmd.help.version"));
        sender.sendMessage(I18n.as("youercmd.help.packetstats"));
        sender.sendMessage(I18n.as("youercmd.help.heal"));
        sender.sendMessage(I18n.as("youercmd.help.speed"));
        sender.sendMessage(I18n.as("youercmd.help.cleardropitem"));
        sender.sendMessage(I18n.as("youercmd.help.memoryfix"));
        sender.sendMessage(I18n.as("youercmd.help.channels_incom"));
        sender.sendMessage(I18n.as("youercmd.help.channels_outgo"));
        sender.sendMessage(I18n.as("youercmd.help.modchannels"));
        sender.sendMessage(I18n.as("youercmd.help.printthreadcost"));
        sender.sendMessage(I18n.as("youercmd.help.help"));
        sender.sendMessage(I18n.as("youercmd.help.backupworld"));
        sender.sendMessage(I18n.as("youercmd.help.migratedb"));
    }

    private static void migrateAll(CommandSender sender, String target) {
        int totalSucceeded = 0;
        int totalRows = 0;
        int totalCount = 0;

        for (String module : DatabaseMigration.getModuleNames()) {
            sender.sendMessage(I18n.as("migratedb.starting",
                    DatabaseMigration.getDisplayName(module), target));
            DatabaseMigration.MigrationResult result = DatabaseMigration.migrate(module, target);
            totalCount++;
            if (result.success()) {
                totalSucceeded++;
                totalRows += result.rows();
                sender.sendMessage(I18n.as("migratedb.success",
                        DatabaseMigration.getDisplayName(module), result.rows()));
            } else {
                sender.sendMessage(I18n.as("migratedb.fail",
                        DatabaseMigration.getDisplayName(module), result.message()));
            }
        }

        sender.sendMessage(I18n.as("migratedb.all.complete",
                totalSucceeded, totalCount, totalRows));
    }
}
