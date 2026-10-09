package com.mohistmc.youer.commands;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.pulsegrasp.PulseGrasp;
import com.mohistmc.youer.util.I18n;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PulseGraspCommand extends Command {

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

        // PulseGrasp 的报告与结论面向中文用户；非中文服务器直接提示自行安装 spark。
        if (!YouerConfig.isChinese()) {
            sender.sendMessage(I18n.as("pulsegrasp.not.chinese"));
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("start")) {
            sendHelp(sender);
            return false;
        }
        if (args.length > 1) {
            sender.sendMessage(I18n.as("pulsegrasp.start.noargs"));
            return false;
        }
        if (PulseGrasp.isGrasping()) {
            sender.sendMessage(I18n.as("pulsegrasp.already.running"));
            return true;
        }
        if (!PulseGrasp.start(sender.getName(), recorderUuid(sender))) {
            sender.sendMessage(I18n.as("pulsegrasp.cooldown", String.valueOf(PulseGrasp.cooldownSeconds())));
            return true;
        }

        sender.sendMessage(I18n.as("pulsegrasp.started", String.valueOf(PulseGrasp.durationSeconds())));
        return true;
    }

    /** 输出多行 help 列表（按 \n 拆行逐条发送，避免某些客户端不渲染嵌入换行） */
    private static void sendHelp(CommandSender sender) {
        for (String line : I18n.as("pulsegrasp.help").split("\n")) {
            sender.sendMessage(line);
        }
    }

    /** 获取记录者 UUID — 玩家返回真实 UUID，控制台等非玩家返回 none（none 不显示进度） */
    private static String recorderUuid(CommandSender sender) {
        return sender instanceof org.bukkit.entity.Player player ? player.getUniqueId().toString() : "none";
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (!YouerConfig.isChinese()) {
            return List.of(); // 非中文服务器不提示任何子命令
        }
        // "start" 是唯一的子命令
        if (args.length == 1 && "start".startsWith(args[0].toLowerCase())) {
            return List.of("start");
        }
        return List.of();
    }
}
