package com.mohistmc.youer.feature.plugin;

import com.mohistmc.youer.util.I18n;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PluginHotReloadCommand extends Command {

    private static final String[] COMMANDS = {
            "reload", "load", "unload", "list"
    };

    public PluginHotReloadCommand(String name) {
        super(name);
        this.description = I18n.as("plugin.description");
        this.usageMessage = "/" + name + " reload <plugin> [newJar] | load <jarPath> | unload <plugin> | list";
        this.setPermission("youer.command.plugin");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) return true;
        if (args.length == 0) {
            sender.sendMessage(I18n.as("plugin.usage", usageMessage));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender, args);
            case "load" -> handleLoad(sender, args);
            case "unload" -> handleUnload(sender, args);
            case "list" -> handleList(sender);
            default -> sender.sendMessage(I18n.as("plugin.unknown", args[0]));
        }
        return true;
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(I18n.as("plugin.reload.usage"));
            return;
        }
        String pluginName = args[1];
        sender.sendMessage(I18n.as("plugin.reload.start", pluginName));

        Plugin result;
        if (args.length >= 3) {
            result = PluginHotReloadUtil.reload(pluginName, resolveJar(args[2]));
        } else {
            result = PluginHotReloadUtil.reload(pluginName);
        }

        if (result != null) {
            sender.sendMessage(I18n.as("plugin.reload.success",
                result.getName(), result.getPluginMeta().getVersion()));
        } else {
            sender.sendMessage(I18n.as("plugin.reload.fail", pluginName));
        }
    }

    private void handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(I18n.as("plugin.load.usage"));
            return;
        }
        File jarFile = resolveJar(args[1]);
        if (!jarFile.exists()) {
            sender.sendMessage(I18n.as("plugin.load.notfound", args[1]));
            return;
        }

        // Check if this plugin is already loaded
        String name = PluginLoadUtil.detectPluginName(jarFile);
        if (name != null && PluginHotReloadUtil.isPluginEnabled(name)) {
            sender.sendMessage(I18n.as("plugin.load.duplicate", name));
            return;
        }

        sender.sendMessage(I18n.as("plugin.load.start", jarFile.getName()));
        Plugin plugin = PluginHotReloadUtil.loadAndEnable(jarFile);

        if (plugin != null) {
            sender.sendMessage(I18n.as("plugin.load.success",
                plugin.getName(), plugin.getPluginMeta().getVersion()));
        } else {
            sender.sendMessage(I18n.as("plugin.load.fail"));
        }
    }

    private void handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(I18n.as("plugin.unload.usage"));
            return;
        }
        String pluginName = args[1];
        sender.sendMessage(I18n.as("plugin.unload.start", pluginName));
        if (PluginHotReloadUtil.unload(pluginName)) {
            sender.sendMessage(I18n.as("plugin.unload.success", pluginName));
        } else {
            sender.sendMessage(I18n.as("plugin.unload.fail", pluginName));
        }
    }

    private void handleList(CommandSender sender) {
        Plugin[] plugins = PluginHotReloadUtil.getPlugins();
        sender.sendMessage(I18n.as("plugin.list.header", plugins.length));
        for (Plugin plugin : plugins) {
            sender.sendMessage(I18n.as("plugin.list.entry",
                plugin.isEnabled() ? "§a✔" : "§c✘",
                plugin.getName(),
                plugin.getPluginMeta().getVersion()));
        }
    }

    private static File resolveJar(String path) {
        File file = new File(path);
        if (file.exists()) return file;
        // auto-resolve relative path against plugins folder
        File pluginsFolder = Bukkit.getServer().getPluginsFolder();
        return new File(pluginsFolder, path);
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (!testPermission(sender)) return list;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String cmd : COMMANDS) {
                if (cmd.startsWith(prefix)) list.add(cmd);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("reload".equals(sub) || "unload".equals(sub)) {
                String prefix = args[1].toLowerCase();
                for (Plugin plugin : PluginHotReloadUtil.getPlugins()) {
                    if (plugin.getName().toLowerCase().startsWith(prefix)) {
                        list.add(plugin.getName());
                    }
                }
            } else if ("load".equals(sub)) {
                String prefix = args[1].toLowerCase();
                File pluginsFolder = Bukkit.getServer().getPluginsFolder();
                File[] jars = pluginsFolder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".jar"));
                if (jars != null) {
                    for (File jar : jars) {
                        if (jar.getName().toLowerCase().startsWith(prefix)) {
                            list.add(jar.getName());
                        }
                    }
                }
            }
        }
        return list;
    }
}
