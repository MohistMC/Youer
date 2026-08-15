package com.mohistmc.youer.commands;

import com.mohistmc.youer.feature.PluginManager;
import com.mohistmc.youer.util.I18n;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PluginManagerCommand extends Command {

    private static final List<String> SUBCOMMANDS = List.of("load", "reload", "unload");

    public PluginManagerCommand(String name) {
        super(name);
        this.description = I18n.as("plugincmd.description");
        this.setPermission("youer.command.plugin");
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

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "load" -> {
                if (args.length < 2) {
                    sendHelp(sender);
                    return false;
                }
                File file = new File(args[1]);
                if (!file.isAbsolute()) {
                    file = new File("plugins", args[1]);
                }
                sender.sendMessage(PluginManager.load(file));
            }
            case "reload" -> {
                if (args.length < 2) {
                    sendHelp(sender);
                    return false;
                }
                sender.sendMessage(PluginManager.reload(args[1]));
            }
            case "unload" -> {
                if (args.length < 2) {
                    sendHelp(sender);
                    return false;
                }
                sender.sendMessage(PluginManager.unload(args[1]));
            }
            default -> {
                sendHelp(sender);
                return false;
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (String line : I18n.as("plugincmd.help").split("\n")) {
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("load")) {
            File dir = new File("plugins");
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")
                            && file.getName().toLowerCase(Locale.ENGLISH).startsWith(args[1].toLowerCase(Locale.ENGLISH))) {
                        // Skip jars whose plugin identifier is already loaded
                        // (match by plugin.yml name, not file name, due to Paper's remap).
                        String pluginName = PluginManager.pluginNameOfJar(file);
                        if (pluginName != null && Bukkit.getPluginManager().getPlugin(pluginName) != null) {
                            continue;
                        }
                        completions.add(file.getName());
                    }
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("unload"))) {
            for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (plugin.getName().toLowerCase(Locale.ENGLISH).startsWith(args[1].toLowerCase(Locale.ENGLISH))) {
                    completions.add(plugin.getName());
                }
            }
        }
        return completions;
    }
}