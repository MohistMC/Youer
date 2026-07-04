package com.mohistmc.youer.feature.plugin;

import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.LogUtils;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import io.papermc.paper.plugin.provider.type.PluginFileType;
import java.io.File;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

public final class PluginLoadUtil {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    private static void logProgress(String msg) {
        if (PluginHotReloadUtil.isReloading()) {
            LOGGER.debug(msg);
        } else {
            LOGGER.info(msg);
        }
    }

    private PluginLoadUtil() {
    }

    /**
     * Read the plugin name from a JAR without fully loading it.
     * Returns null if the JAR is not a valid plugin.
     */
    public static String detectPluginName(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.getName().endsWith(".jar")) return null;
        try (JarFile jar = new JarFile(jarFile, true, JarFile.OPEN_READ, JarFile.runtimeVersion())) {
            PluginFileType<?, ?> type = PluginFileType.guessType(jar);
            if (type == null) return null;
            PluginMeta meta = type.getConfig(jar);
            return meta != null ? meta.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Plugin loadPlugin(File jarFile) {
        if (jarFile == null) throw new IllegalArgumentException("jarFile must not be null");
        if (!jarFile.exists()) throw new IllegalArgumentException(I18n.as("plugin.loadutil.notexists", jarFile.getAbsolutePath()));
        if (!jarFile.getName().endsWith(".jar")) throw new IllegalArgumentException(I18n.as("plugin.loadutil.notjar", jarFile.getAbsolutePath()));

        // Pre-check: if a plugin with the same name is already loaded, refuse
        String name = detectPluginName(jarFile);
        if (name != null && PaperPluginManagerImpl.getInstance().getPlugin(name) != null) {
            LOGGER.warn(I18n.as("plugin.loadutil.duplicate", name, jarFile.getAbsolutePath()));
            return null;
        }

        logProgress(I18n.as("plugin.loadutil.loading", jarFile.getAbsolutePath()));

        try {
            Plugin plugin = PaperPluginManagerImpl.getInstance().loadPlugin(jarFile);
            if (plugin == null) {
                LOGGER.error(I18n.as("plugin.loadutil.null", jarFile.getAbsolutePath()));
                return null;
            }
            logProgress(I18n.as("plugin.loadutil.success", plugin.getName(), plugin.getPluginMeta().getVersion()));
            return plugin;
        } catch (InvalidPluginException e) {
            LOGGER.error(I18n.as("plugin.loadutil.invalid", jarFile.getAbsolutePath(), e.getMessage()));
            LOGGER.debug(I18n.as("plugin.loadutil.detail"), e);
            return null;
        } catch (Exception e) {
            LOGGER.error(I18n.as("plugin.loadutil.error", jarFile.getAbsolutePath()), e);
            return null;
        }
    }

    public static Plugin loadPlugin(Path jarPath) {
        if (jarPath == null) throw new IllegalArgumentException("jarPath must not be null");
        return loadPlugin(jarPath.toFile());
    }

    public static boolean enablePlugin(Plugin plugin) {
        if (plugin == null) return false;
        if (plugin.isEnabled()) {
            LOGGER.warn(I18n.as("plugin.loadutil.alreadyenabled", plugin.getName()));
            return false;
        }
        logProgress(I18n.as("plugin.loadutil.enabling", plugin.getName()));
        try {
            PaperPluginManagerImpl.getInstance().enablePlugin(plugin);
            logProgress(I18n.as("plugin.loadutil.enabled", plugin.getName()));
            return true;
        } catch (Throwable e) {
            LOGGER.error(I18n.as("plugin.loadutil.enablefail", plugin.getName()), e);
            return false;
        }
    }

    public static Plugin loadAndEnablePlugin(File jarFile) {
        Plugin plugin = loadPlugin(jarFile);
        if (plugin == null) return null;
        if (enablePlugin(plugin)) return plugin;
        PluginUnloadUtil.unloadPlugin(plugin);
        return null;
    }
}
