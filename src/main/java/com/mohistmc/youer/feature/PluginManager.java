package com.mohistmc.youer.feature;

import com.mohistmc.youer.util.I18n;
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hot-load / hot-reload / unload Bukkit plugins at runtime.
 *
 * <p>Implemented on top of Paper's {@link PaperPluginManagerImpl} so that plugin
 * class loaders, dependency trees and lifecycle events are managed natively.</p>
 */
public final class PluginManager {

    private PluginManager() {
    }

    /**
     * Hot-load a plugin from a file and enable it.
     *
     * @param file the plugin jar file
     * @return a localized result message
     */
    public static String load(File file) {
        if (file == null || !file.isFile()) {
            return I18n.as("pluginmanager.load.notfound", file == null ? "null" : file.getAbsolutePath());
        }
        // Abort if the jar's plugin is already loaded; loading it again would make
        // Paper reject the duplicate plugin identifier and clutter the console.
        // Match by plugin identifier (parsed from plugin.yml) rather than file name,
        // because Paper's remapper renames the loaded file with a timestamp suffix.
        String pluginName = pluginNameOfJar(file);
        if (pluginName != null && PaperPluginManagerImpl.getInstance().getPlugin(pluginName) != null) {
            return I18n.as("pluginmanager.load.already", pluginName);
        }
        try {
            Plugin plugin = PaperPluginManagerImpl.getInstance().loadPlugin(file);
            if (plugin == null) {
                return I18n.as("pluginmanager.load.failed", file.getName());
            }
            PaperPluginManagerImpl.getInstance().enablePlugin(plugin);
            return I18n.as("pluginmanager.load.success", plugin.getName());
        } catch (Exception e) {
            return I18n.as("pluginmanager.load.error", file.getName(), String.valueOf(e.getMessage()));
        }
    }

    /**
     * Hot-reload an already loaded plugin: disable it, drop its stale instance,
     * then reload and re-enable it from the same file.
     *
     * @param name the plugin name
     * @return a localized result message
     */
    public static String reload(String name) {
        Plugin plugin = PaperPluginManagerImpl.getInstance().getPlugin(name);
        if (plugin == null) {
            return I18n.as("pluginmanager.notfound", name);
        }
        File file = plugin instanceof JavaPlugin javaPlugin ? originalJar(plugin, javaPlugin.getFile()) : null;
        if (file == null) {
            return I18n.as("pluginmanager.nofile", name);
        }
        unregisterCommands(plugin);
        PaperPluginManagerImpl.getInstance().disablePlugin(plugin);
        removeFromInternalLists(plugin);
        try {
            Plugin loaded = PaperPluginManagerImpl.getInstance().loadPlugin(file);
            if (loaded == null) {
                return I18n.as("pluginmanager.load.failed", file.getName());
            }
            PaperPluginManagerImpl.getInstance().enablePlugin(loaded);
            return I18n.as("pluginmanager.reload.success", loaded.getName());
        } catch (Exception e) {
            return I18n.as("pluginmanager.reload.error", name, String.valueOf(e.getMessage()));
        }
    }

    /**
     * Unload a plugin: disable it and drop its stale instance so it disappears
     * from the plugin list until the next server start.
     *
     * @param name the plugin name
     * @return a localized result message
     */
    public static String unload(String name) {
        Plugin plugin = PaperPluginManagerImpl.getInstance().getPlugin(name);
        if (plugin == null) {
            return I18n.as("pluginmanager.notfound", name);
        }
        unregisterCommands(plugin);
        PaperPluginManagerImpl.getInstance().disablePlugin(plugin);
        removeFromInternalLists(plugin);
        return I18n.as("pluginmanager.unload.success", name);
    }

    /**
     * Paper/Mohist remap plugin jars into {@code plugins/.paper-remapped/} at load time, so
     * {@link JavaPlugin#getFile()} points at that stale remapped copy instead of the jar the
     * user actually edits. The remapped copy is named {@code <name>-<timestamp>.jar} in the
     * {@code unknown-origin} sub-folder, so its file name cannot be used to locate the original.
     * Match the original jar in the plugins directory by the plugin identifier declared in
     * plugin.yml instead, so reload picks up the new code; {@code PaperPluginManagerImpl#loadPlugin}
     * will re-remap it whenever its content has changed.
     */
    private static File originalJar(Plugin plugin, File file) {
        if (file == null) return null;
        String abs = file.getAbsolutePath().replace('\\', '/');
        if (!abs.contains("/.paper-remapped/")) {
            return file;
        }
        File pluginsDir = new File("plugins");
        File[] jars = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars != null) {
            String pluginName = plugin.getName();
            for (File jar : jars) {
                if (pluginName.equals(pluginNameOfJar(jar))) {
                    return jar;
                }
            }
        }
        // Fallback: non-timestamped copies share the original file name.
        File original = new File(pluginsDir, file.getName());
        return original.isFile() ? original : file;
    }

    /**
     * Remove every command owned by the given plugin from the global command map,
     * so they do not collide with the freshly re-registered commands on reload.
     */
    private static void unregisterCommands(Plugin plugin) {
        CommandMap commandMap = Bukkit.getServer().getCommandMap();
        Map<String, Command> known = commandMap.getKnownCommands();
        List<Command> owned = new ArrayList<>();
        // The command map may be a forwarding map whose entrySet iterator does not
        // support remove(); collect owned commands first, then remove them by name.
        for (Command command : known.values()) {
            if (command instanceof PluginCommand pluginCommand && pluginCommand.getPlugin() == plugin) {
                owned.add(command);
            }
        }
        for (Command command : owned) {
            command.unregister(commandMap);
            known.remove(command.getName());
            for (String alias : command.getAliases()) {
                known.remove(alias);
            }
        }
    }

    /**
     * Drop the (now disabled) plugin instance from Paper's internal plugin list
     * and name lookup map so a reloaded/unloaded plugin does not leave a stale
     * entry behind.
     */
    private static void removeFromInternalLists(Plugin plugin) {
        try {
            PaperPluginManagerImpl impl = PaperPluginManagerImpl.getInstance();
            Field instanceManager = PaperPluginManagerImpl.class.getDeclaredField("instanceManager");
            instanceManager.setAccessible(true);
            Object im = instanceManager.get(impl);

            Field plugins = im.getClass().getDeclaredField("plugins");
            plugins.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Plugin> pluginList = (List<Plugin>) plugins.get(im);
            pluginList.remove(plugin);

            Field lookupNames = im.getClass().getDeclaredField("lookupNames");
            lookupNames.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Plugin> lookup = (Map<String, Plugin>) lookupNames.get(im);
            lookup.values().remove(plugin);
        } catch (Exception ignored) {
            // Best-effort cleanup; failing to remove the stale entry is non-fatal.
        }
    }

    /**
     * Read the plugin identifier from a jar's {@code plugin.yml}.
     *
     * @param file the plugin jar file
     * @return the plugin name, or {@code null} if it cannot be determined
     */
    public static String pluginNameOfJar(File file) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(file)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) {
                return null;
            }
            try (java.io.InputStream in = jar.getInputStream(entry)) {
                org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration
                        .loadConfiguration(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                String name = yaml.getString("name");
                return name != null && !name.isEmpty() ? name : null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}