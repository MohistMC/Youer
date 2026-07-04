package com.mohistmc.youer.feature.plugin;

import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.LogUtils;
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

public final class PluginHotReloadUtil {

    private static final Logger LOGGER = LogUtils.getClassLogger();
    static final ThreadLocal<Boolean> RELOADING = ThreadLocal.withInitial(() -> false);

    static boolean isReloading() {
        return RELOADING.get();
    }

    private PluginHotReloadUtil() {
    }

    public static Plugin[] getPlugins() {
        return PaperPluginManagerImpl.getInstance().getPlugins();
    }

    public static Plugin getPlugin(String name) {
        return PaperPluginManagerImpl.getInstance().getPlugin(name);
    }

    public static boolean isPluginEnabled(String name) {
        return PaperPluginManagerImpl.getInstance().isPluginEnabled(name);
    }

    public static List<String> getPluginNames() {
        return Arrays.stream(getPlugins())
            .map(Plugin::getName)
            .filter(Objects::nonNull)
            .toList();
    }

    public static boolean unload(String pluginName) {
        return PluginUnloadUtil.unloadPlugin(pluginName);
    }

    public static boolean unload(Plugin plugin) {
        return PluginUnloadUtil.unloadPlugin(plugin);
    }

    public static Plugin load(File jarFile) {
        return PluginLoadUtil.loadPlugin(jarFile);
    }

    public static Plugin loadAndEnable(File jarFile) {
        return PluginLoadUtil.loadAndEnablePlugin(jarFile);
    }

    public static Plugin reload(String pluginName) {
        if (pluginName == null) {
            LOGGER.error(I18n.as("plugin.reloadutil.namenull"));
            return null;
        }
        Plugin existing = PaperPluginManagerImpl.getInstance().getPlugin(pluginName);
        if (existing == null) {
            LOGGER.error(I18n.as("plugin.reloadutil.notfound", pluginName));
            return null;
        }
        File pluginFile = PluginUnloadUtil.getPluginFile(existing);
        if (pluginFile == null || !pluginFile.exists()) {
            LOGGER.error(I18n.as("plugin.reloadutil.nofile", pluginName));
            return null;
        }
        return reload(pluginName, pluginFile);
    }

    public static Plugin reload(String pluginName, File newJarFile) {
        if (pluginName == null || newJarFile == null) {
            LOGGER.error(I18n.as("plugin.reloadutil.namenull"));
            return null;
        }
        if (!newJarFile.exists()) {
            LOGGER.error(I18n.as("plugin.reloadutil.jar404", pluginName, newJarFile.getAbsolutePath()));
            return null;
        }

        RELOADING.set(true);
        try {
            Plugin existing = PaperPluginManagerImpl.getInstance().getPlugin(pluginName);
            if (existing != null) {
                PluginUnloadUtil.unloadPlugin(existing);
            }

            Plugin plugin = PluginLoadUtil.loadPlugin(newJarFile);
            if (plugin == null) {
                LOGGER.error(I18n.as("plugin.reloadutil.loadfail", pluginName));
                return null;
            }
            if (!PluginLoadUtil.enablePlugin(plugin)) {
                LOGGER.error(I18n.as("plugin.reloadutil.enablefail", pluginName));
                PluginUnloadUtil.unloadPlugin(plugin);
                return null;
            }
            return plugin;
        } finally {
            RELOADING.remove();
        }
    }

    public static void printPluginList() {
        Plugin[] plugins = getPlugins();
        LOGGER.info(I18n.as("plugin.reloadutil.listheader", plugins.length));
        for (Plugin plugin : plugins) {
            LOGGER.info(I18n.as("plugin.reloadutil.listentry",
                plugin.getName(),
                plugin.getPluginMeta().getVersion(),
                plugin.isEnabled() ? I18n.as("plugin.reloadutil.enabled") : I18n.as("plugin.reloadutil.disabled")));
        }
    }
}
