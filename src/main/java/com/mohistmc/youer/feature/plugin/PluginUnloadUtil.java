package com.mohistmc.youer.feature.plugin;

import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.LogUtils;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.dependency.MetaDependencyTree;
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.slf4j.Logger;

public final class PluginUnloadUtil {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    private static void logProgress(String msg) {
        if (PluginHotReloadUtil.isReloading()) {
            LOGGER.debug(msg);
        } else {
            LOGGER.info(msg);
        }
    }

    private static Field instanceManagerField;
    private static Object paperInstanceManager;
    private static Field pluginsListField;
    private static Field lookupNamesField;
    private static Field dependencyTreeField;
    private static final Object REF_LOCK = new Object();
    private static volatile boolean reflectionReady = false;
    private static boolean reflectionAttempted = false;

    private static boolean ensureReflection() {
        if (reflectionReady) return true;
        if (reflectionAttempted) return false;
        synchronized (REF_LOCK) {
            if (reflectionReady) return true;
            if (reflectionAttempted) return false;
            reflectionAttempted = true;
            try {
                instanceManagerField = PaperPluginManagerImpl.class.getDeclaredField("instanceManager");
                instanceManagerField.setAccessible(true);

                Class<?> clz = Class.forName("io.papermc.paper.plugin.manager.PaperPluginInstanceManager");
                pluginsListField = clz.getDeclaredField("plugins");
                pluginsListField.setAccessible(true);
                lookupNamesField = clz.getDeclaredField("lookupNames");
                lookupNamesField.setAccessible(true);
                dependencyTreeField = clz.getDeclaredField("dependencyTree");
                dependencyTreeField.setAccessible(true);

                try {
                    PaperPluginManagerImpl pm = PaperPluginManagerImpl.getInstance();
                    paperInstanceManager = instanceManagerField.get(pm);
                } catch (Exception ignored) {
                }

                reflectionReady = true;
                return true;
            } catch (Exception e) {
                LOGGER.error(I18n.as("plugin.unloadutil.reflection.fail"), e);
                return false;
            }
        }
    }

    public static boolean unloadPlugin(String pluginName) {
        Plugin plugin = PaperPluginManagerImpl.getInstance().getPlugin(pluginName);
        if (plugin == null) {
            LOGGER.warn(I18n.as("plugin.unloadutil.notfound", pluginName));
            return false;
        }
        return unloadPlugin(plugin);
    }

    public static boolean unloadPlugin(Plugin plugin) {
        if (plugin == null) return false;
        String name = plugin.getName();
        logProgress(I18n.as("plugin.unloadutil.start", name));

        try {
            PaperPluginManagerImpl.getInstance().disablePlugin(plugin);
        } catch (Exception e) {
            LOGGER.error(I18n.as("plugin.unloadutil.error.disable", name), e);
        }
        try {
            removeFromInstanceManager(plugin);
        } catch (Exception e) {
            LOGGER.error(I18n.as("plugin.unloadutil.error.manager", name), e);
        }
        try {
            removePluginCommands(plugin);
        } catch (Exception e) {
            LOGGER.error(I18n.as("plugin.unloadutil.error.commands", name), e);
        }
        try {
            removePluginPermissions(plugin);
        } catch (Exception e) {
            LOGGER.error(I18n.as("plugin.unloadutil.error.permissions", name), e);
        }

        logProgress(I18n.as("plugin.unloadutil.success", name));
        return true;
    }

    public static File getPluginFile(Plugin plugin) {
        if (plugin == null) return null;
        try {
            java.lang.reflect.Method getFile = org.bukkit.plugin.java.JavaPlugin.class
                .getDeclaredMethod("getFile");
            getFile.setAccessible(true);
            return (File) getFile.invoke(plugin);
        } catch (Exception e) {
            LOGGER.debug(I18n.as("plugin.unloadutil.getfile.fail", plugin.getName()), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeFromInstanceManager(Plugin plugin) throws Exception {
        if (!ensureReflection()) return;

        PaperPluginManagerImpl pm = PaperPluginManagerImpl.getInstance();
        Object instanceManager = instanceManagerField.get(pm);
        if (instanceManager != paperInstanceManager) {
            paperInstanceManager = instanceManager;
        }

        Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(instanceManager);
        String nameLower = plugin.getName().toLowerCase(Locale.ENGLISH);
        lookupNames.remove(nameLower);

        PluginMeta meta = plugin.getPluginMeta();
        if (meta != null) {
            for (String provided : meta.getProvidedPlugins()) {
                lookupNames.remove(provided.toLowerCase(Locale.ENGLISH));
            }
        }

        List<Plugin> pluginsList = (List<Plugin>) pluginsListField.get(instanceManager);
        pluginsList.remove(plugin);

        MetaDependencyTree depTree = (MetaDependencyTree) dependencyTreeField.get(instanceManager);
        if (meta != null && depTree != null) {
            depTree.remove(meta);
        }
    }

    private static void removePluginCommands(Plugin plugin) {
        CraftServer server = (CraftServer) Bukkit.getServer();
        CommandMap commandMap = server.getCommandMap();
        if (!(commandMap instanceof SimpleCommandMap simpleCommandMap)) return;

        Map<String, Command> knownCommands = simpleCommandMap.getKnownCommands();
        if (knownCommands == null || knownCommands.isEmpty()) return;

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            if (entry.getValue() instanceof PluginCommand pc && plugin.equals(pc.getPlugin())) {
                toRemove.add(entry.getKey());
            }
        }
        for (String key : toRemove) {
            Command cmd = knownCommands.remove(key);
            if (cmd != null) {
                try {
                    cmd.unregister(commandMap);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void removePluginPermissions(Plugin plugin) {
        PluginMeta meta = plugin.getPluginMeta();
        if (!(meta instanceof PluginDescriptionFile desc)) return;

        PaperPluginManagerImpl pm = PaperPluginManagerImpl.getInstance();
        for (Permission perm : desc.getPermissions()) {
            try {
                if (pm.getPermission(perm.getName()) != null) {
                    pm.removePermission(perm.getName());
                }
            } catch (Exception ignored) {
            }
        }
    }
}
