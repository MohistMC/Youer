package com.mohistmc.youer.commands;

import com.mohistmc.youer.feature.logfilter.LogFilterConfig;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class LogFilterCommand extends Command {

    private static final List<String> SUBCOMMANDS = List.of("list", "reload", "on", "off");

    public LogFilterCommand(String name) {
        super(name);
        this.description = I18n.as("logfiltercmd.description");
        this.setPermission("youer.command.logfilter");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return false;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "on" -> setEnabled(sender, true);
            case "off" -> setEnabled(sender, false);
            default -> {
                sendHelp(sender);
                return false;
            }
        }
        return true;
    }

    private void list(CommandSender sender) {
        List<String> filters = LogFilterConfig.INSTANCE.getFilters();
        if (filters.isEmpty()) {
            sender.sendMessage(I18n.as("logfiltercmd.list.empty"));
            return;
        }
        sender.sendMessage(I18n.as("logfiltercmd.list.header"));
        for (int i = 0; i < filters.size(); i++) {
            sender.sendMessage(I18n.as("logfiltercmd.list.entry", i + 1, filters.get(i)));
        }
    }

    private void reload(CommandSender sender) {
        LogFilterConfig.INSTANCE.apply();
        sender.sendMessage(I18n.as("logfiltercmd.reload.success"));
    }

    private void setEnabled(CommandSender sender, boolean enabled) {
        LogFilterConfig.INSTANCE.setEnabled(enabled);
        sender.sendMessage(enabled
                ? I18n.as("logfiltercmd.on")
                : I18n.as("logfiltercmd.off"));
    }

    private void sendHelp(CommandSender sender) {
        for (String line : I18n.as("logfiltercmd.help").split("\n")) {
            sender.sendMessage(line);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String cmd : SUBCOMMANDS) {
                if (cmd.startsWith(args[0].toLowerCase(Locale.ENGLISH))) {
                    completions.add(cmd);
                }
            }
        }
        return completions;
    }
}
