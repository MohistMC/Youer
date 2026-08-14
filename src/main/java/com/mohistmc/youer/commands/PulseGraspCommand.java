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
        this.usageMessage = I18n.as("pulsegrasp.usage");
        this.setPermission("youer.command.pulsegrasp");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(I18n.as("pulsegrasp.usage"));
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (PulseGrasp.isGrasping()) {
                    sender.sendMessage(I18n.as("pulsegrasp.already.running"));
                    return true;
                }
                PulseGrasp.start(sender.getName(), recorderUuid(sender));
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
                sender.sendMessage(I18n.as("pulsegrasp.unknown.subcommand") + " " + usageMessage);
                return false;
            }
        }
        return true;
    }

    /** 获取记录者 UUID — 玩家返回真实 UUID，控制台等非玩家返回 none */
    private static String recorderUuid(CommandSender sender) {
        return sender instanceof org.bukkit.entity.Player player ? player.getUniqueId().toString() : "none";
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