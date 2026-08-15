package com.mohistmc.youer.commands;

import com.mohistmc.youer.feature.pulsegrasp.PulseGrasp;
import com.mohistmc.youer.util.I18n;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PulseGraspCommand extends Command {

    private static final List<String> SUBCOMMANDS = List.of("start", "stop", "status");

    public PulseGraspCommand(String name) {
        super(name);
        this.description = I18n.as("pulsegrasp.description");
        this.usageMessage = I18n.as("pulsegrasp.help");
        this.setPermission("youer.command.pulsegrasp");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (PulseGrasp.isGrasping()) {
                    sender.sendMessage(I18n.as("pulsegrasp.already.running"));
                    return true;
                }
                PulseGrasp.SampleOptions opts = parseStartOptions(sender, args);
                if (opts == null) {
                    return false; // 参数解析失败，错误信息已发送
                }
                PulseGrasp.start(sender.getName(), recorderUuid(sender), opts);
                sender.sendMessage(I18n.as("pulsegrasp.started"));
            }
            case "stop" -> {
                if (!PulseGrasp.isGrasping()) {
                    sender.sendMessage(I18n.as("pulsegrasp.not.running"));
                    return true;
                }
                int ticks = PulseGrasp.getTickCount();
                PulseGrasp.stop(sender.getName(), recorderUuid(sender));
                sender.sendMessage(I18n.as("pulsegrasp.generating", String.valueOf(ticks)));
            }
            case "status" -> {
                if (PulseGrasp.isGrasping()) {
                    sender.sendMessage(I18n.as("pulsegrasp.status.running", String.valueOf(PulseGrasp.getTickCount())));
                } else {
                    sender.sendMessage(I18n.as("pulsegrasp.status.idle"));
                }
            }
            default -> {
                sendHelp(sender);
                return false;
            }
        }
        return true;
    }

    /** 输出多行 help 列表（按 \n 拆行逐条发送，避免某些客户端不渲染嵌入换行） */
    private static void sendHelp(CommandSender sender) {
        for (String line : I18n.as("pulsegrasp.help").split("\n")) {
            sender.sendMessage(line);
        }
    }

    /** 获取记录者 UUID — 玩家返回真实 UUID，控制台等非玩家返回 none */
    private static String recorderUuid(CommandSender sender) {
        return sender instanceof org.bukkit.entity.Player player ? player.getUniqueId().toString() : "none";
    }

    /**
     * 解析 start 子命令的可选采样参数（仿 spark 语法）。
     * <ul>
     *   <li>--interval_ms &lt;ms&gt;  采样间隔（毫秒）</li>
     *   <li>--depth &lt;n&gt;         栈抓取最大深度</li>
     *   <li>--top_n &lt;n&gt;         报告中每个热点的最大方法数</li>
     * </ul>
     * 未指定的字段保持 null，回退到 MethodSampler 默认值。解析失败返回 null（错误信息已发送）。
     */
    private static PulseGrasp.SampleOptions parseStartOptions(CommandSender sender, String[] args) {
        Long intervalMs = null;
        Integer maxDepth = null;
        Integer maxTopN = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--interval_ms", "--interval" -> {
                    if (i + 1 >= args.length) return invalidOption(sender, args[i]);
                    intervalMs = parseLong(sender, args[++i], "--interval_ms");
                    if (intervalMs == null) return null;
                }
                case "--depth" -> {
                    if (i + 1 >= args.length) return invalidOption(sender, args[i]);
                    maxDepth = parseInt(sender, args[++i], "--depth");
                    if (maxDepth == null) return null;
                }
                case "--top_n", "--top-n" -> {
                    if (i + 1 >= args.length) return invalidOption(sender, args[i]);
                    maxTopN = parseInt(sender, args[++i], "--top_n");
                    if (maxTopN == null) return null;
                }
                default -> {
                    sender.sendMessage(I18n.as("pulsegrasp.unknown.option", args[i]));
                    return null;
                }
            }
        }
        return new PulseGrasp.SampleOptions(intervalMs, maxDepth, maxTopN);
    }

    private static Long parseLong(CommandSender sender, String value, String option) {
        try {
            long l = Long.parseLong(value);
            if (l <= 0) throw new NumberFormatException();
            return l;
        } catch (NumberFormatException e) {
            sender.sendMessage(I18n.as("pulsegrasp.invalid.value", option, value));
            return null;
        }
    }

    private static Integer parseInt(CommandSender sender, String value, String option) {
        try {
            int i = Integer.parseInt(value);
            if (i <= 0) throw new NumberFormatException();
            return i;
        } catch (NumberFormatException e) {
            sender.sendMessage(I18n.as("pulsegrasp.invalid.value", option, value));
            return null;
        }
    }

    private static PulseGrasp.SampleOptions invalidOption(CommandSender sender, String option) {
        sender.sendMessage(I18n.as("pulsegrasp.missing.value", option));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}