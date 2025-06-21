package io.papermc.paper.plugin.manager;

import com.google.common.base.Preconditions;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.mohistmc.youer.util.I18n;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.Entrypoint;
import io.papermc.paper.plugin.entrypoint.dependency.MetaDependencyTree;
import io.papermc.paper.plugin.entrypoint.dependency.SimpleMetaDependencyTree;
import io.papermc.paper.plugin.entrypoint.strategy.PluginGraphCycleException;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PaperClassLoaderStorage;
import io.papermc.paper.plugin.provider.source.DirectoryProviderSource;
import io.papermc.paper.plugin.provider.source.FileArrayProviderSource;
import io.papermc.paper.plugin.provider.source.FileProviderSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
class PaperPluginInstanceManager {

    private static final FileProviderSource FILE_PROVIDER_SOURCE = new FileProviderSource("File '%s'"::formatted);

    private final List<Plugin> plugins = new ArrayList<>();
    private final Map<String, Plugin> lookupNames = new HashMap<>();

    private final PluginManager pluginManager;
    private final CommandMap commandMap;
    private final Server server;

    private final MetaDependencyTree dependencyTree = new SimpleMetaDependencyTree(GraphBuilder.directed().build());

    public PaperPluginInstanceManager(PluginManager pluginManager, CommandMap commandMap, Server server) {
        this.commandMap = commandMap;
        this.server = server;
        this.pluginManager = pluginManager;
    }

    public @Nullable Plugin getPlugin(@NotNull String name) {
        return this.lookupNames.get(name.replace(' ', '_').toLowerCase(java.util.Locale.ENGLISH)); // Paper
    }

    public @NotNull Plugin[] getPlugins() {
        return this.plugins.toArray(new Plugin[0]);
    }

    public boolean isPluginEnabled(@NotNull String name) {
        Plugin plugin = this.getPlugin(name);

        return this.isPluginEnabled(plugin);
    }

    public synchronized boolean isPluginEnabled(@Nullable Plugin plugin) {
        if ((plugin != null) && (this.plugins.contains(plugin))) {
            return plugin.isEnabled();
        } else {
            return false;
        }
    }

    public void loadPlugin(Plugin provided) {
        PluginMeta configuration = provided.getPluginMeta();

        this.plugins.add(provided);
        this.lookupNames.put(configuration.getName().toLowerCase(java.util.Locale.ENGLISH), provided);
        for (String providedPlugin : configuration.getProvidedPlugins()) {
            this.lookupNames.putIfAbsent(providedPlugin.toLowerCase(java.util.Locale.ENGLISH), provided);
        }

        this.dependencyTree.add(configuration);
    }

    // InvalidDescriptionException is never used, because the old JavaPluginLoader would wrap the exception.
    public @Nullable Plugin loadPlugin(@NotNull Path path) throws InvalidPluginException, UnknownDependencyException {
        RuntimePluginEntrypointHandler<SingularRuntimePluginProviderStorage> runtimePluginEntrypointHandler = new RuntimePluginEntrypointHandler<>(new SingularRuntimePluginProviderStorage(this.dependencyTree));

        try {
            path = FILE_PROVIDER_SOURCE.prepareContext(path);
            FILE_PROVIDER_SOURCE.registerProviders(runtimePluginEntrypointHandler, path);
        } catch (IllegalArgumentException exception) {
            return null; // Return null when the plugin file is not valid / plugin type is unknown
        } catch (PluginGraphCycleException exception) {
            throw new InvalidPluginException(I18n.as("paperplugininstancemanager.1"));
        } catch (Exception e) {
            throw new InvalidPluginException(e);
        }

        try {
            runtimePluginEntrypointHandler.enter(Entrypoint.PLUGIN);
        } catch (Throwable e) {
            throw new InvalidPluginException(e);
        }

        return runtimePluginEntrypointHandler.getPluginProviderStorage().getSingleLoaded()
            .orElseThrow(() -> new InvalidPluginException(I18n.as("paperplugininstancemanager.2")));
    }

    public @NotNull Plugin[] loadPlugins(@NotNull File[] files) {
        RuntimePluginEntrypointHandler<MultiRuntimePluginProviderStorage> runtimePluginEntrypointHandler = new RuntimePluginEntrypointHandler<>(new MultiRuntimePluginProviderStorage(this.dependencyTree));
        try {
            List<Path> paths = FileArrayProviderSource.INSTANCE.prepareContext(files);
            DirectoryProviderSource.INSTANCE.registerProviders(runtimePluginEntrypointHandler, paths);
            runtimePluginEntrypointHandler.enter(Entrypoint.PLUGIN);
        } catch (Exception e) {
            // This should never happen, any errors that occur in this provider should instead be logged.
            this.server.getLogger().log(Level.SEVERE, I18n.as("paperplugininstancemanager.3"), e);
        }

        return runtimePluginEntrypointHandler.getPluginProviderStorage().getLoaded().toArray(new JavaPlugin[0]);
    }

    // The behavior of this is that all errors are logged instead of being thrown
    public @NotNull Plugin[] loadPlugins(@NotNull Path directory) {
        Preconditions.checkArgument(Files.isDirectory(directory), I18n.as("paperplugininstancemanager.4")); // Avoid creating a directory if it doesn't exist

        RuntimePluginEntrypointHandler<MultiRuntimePluginProviderStorage> runtimePluginEntrypointHandler = new RuntimePluginEntrypointHandler<>(new MultiRuntimePluginProviderStorage(this.dependencyTree));
        try {
            List<Path> files = DirectoryProviderSource.INSTANCE.prepareContext(directory);
            DirectoryProviderSource.INSTANCE.registerProviders(runtimePluginEntrypointHandler, files);
            runtimePluginEntrypointHandler.enter(Entrypoint.PLUGIN);
        } catch (Exception e) {
            // This should never happen, any errors that occur in this provider should instead be logged.
            this.server.getLogger().log(Level.SEVERE, I18n.as("paperplugininstancemanager.5"), e);
        }

        return runtimePluginEntrypointHandler.getPluginProviderStorage().getLoaded().toArray(new JavaPlugin[0]);
    }

    // Plugins are disabled in order like this inorder to "rougly" prevent
    // their dependencies unloading first. But, eh.
    public void disablePlugins() {
        Plugin[] plugins = this.getPlugins();
        for (int i = plugins.length - 1; i >= 0; i--) {
            this.disablePlugin(plugins[i]);
        }
    }

    public void clearPlugins() {
        synchronized (this) {
            this.disablePlugins();
            this.plugins.clear();
            this.lookupNames.clear();
        }
    }

    public synchronized void enablePlugin(@NotNull Plugin plugin) {
        if (plugin.isEnabled()) {
            return;
        }

        if (plugin.getPluginMeta() instanceof PluginDescriptionFile) {
            List<Command> bukkitCommands = PluginCommandYamlParser.parse(plugin);

            if (!bukkitCommands.isEmpty()) {
                this.commandMap.registerAll(plugin.getPluginMeta().getName(), bukkitCommands);
            }
        }

        try {
            String enableMsg = I18n.as("paperplugininstancemanager.6", plugin.getPluginMeta().getDisplayName());
            if (plugin.getPluginMeta() instanceof PluginDescriptionFile descriptionFile && CraftMagicNumbers.isLegacy(descriptionFile)) {
                enableMsg += "*";
            }
            plugin.getLogger().info(enableMsg);

            JavaPlugin jPlugin = (JavaPlugin) plugin;

            if (jPlugin.getClass().getClassLoader() instanceof ConfiguredPluginClassLoader classLoader) { // Paper
                if (PaperClassLoaderStorage.instance().registerUnsafePlugin(classLoader)) {
                    this.server.getLogger().log(Level.WARNING, I18n.as("paperplugininstancemanager.7", plugin.getPluginMeta().getDisplayName()));
                }
            } // Paper

            try {
                jPlugin.setEnabled(true);
            } catch (Throwable ex) {

                this.server.getLogger().log(Level.SEVERE, I18n.as("paperplugininstancemanager.8", plugin.getPluginMeta().getDisplayName()), ex);
                // Paper start - Disable plugins that fail to load
                this.server.getPluginManager().disablePlugin(jPlugin);
                return;
                // Paper end
            }

            // Perhaps abort here, rather than continue going, but as it stands,
            // an abort is not possible the way it's currently written
            this.server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.9", plugin.getPluginMeta().getDisplayName()), ex, plugin);
        }

        HandlerList.bakeAll();
    }

    public synchronized void disablePlugin(@NotNull Plugin plugin) {
        if (!(plugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalArgumentException(I18n.as("paperplugininstancemanager.10"));
        }
        if (!plugin.isEnabled()) {
            return;
        }

        String pluginName = plugin.getPluginMeta().getDisplayName();

        try {
            plugin.getLogger().info(I18n.as("paperplugininstancemanager.11", pluginName));

            this.server.getPluginManager().callEvent(new PluginDisableEvent(plugin));
            try {
                javaPlugin.setEnabled(false);
            } catch (Throwable ex) {
                this.server.getLogger().log(Level.SEVERE, I18n.as("paperplugininstancemanager.12", pluginName), ex);
            }

            ClassLoader classLoader = plugin.getClass().getClassLoader();
            if (classLoader instanceof ConfiguredPluginClassLoader configuredPluginClassLoader) {
                try {
                    configuredPluginClassLoader.close();
                } catch (IOException ex) {
                    this.server.getLogger().log(Level.WARNING, I18n.as("paperplugininstancemanager.13", pluginName), ex); // Paper - log exception
                }
                // Remove from the classloader pool inorder to prevent plugins from trying
                // to access classes
                PaperClassLoaderStorage.instance().unregisterClassloader(configuredPluginClassLoader);
            }

        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.14", pluginName), ex, plugin); // Paper
        }

        try {
            this.server.getScheduler().cancelTasks(plugin);
        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.15", pluginName), ex, plugin); // Paper
        }

        // Paper start - Folia schedulers
        try {
            this.server.getGlobalRegionScheduler().cancelTasks(plugin);
        } catch (Throwable ex) {
            this.handlePluginException("Error occurred (in the plugin loader) while cancelling global tasks for "
                    + pluginName + " (Is it up to date?)", ex, plugin); // Paper
        }

        try {
            this.server.getAsyncScheduler().cancelTasks(plugin);
        } catch (Throwable ex) {
            this.handlePluginException("Error occurred (in the plugin loader) while cancelling async tasks for "
                    + pluginName + " (Is it up to date?)", ex, plugin); // Paper
        }
        // Paper end - Folia schedulers

        try {
            this.server.getServicesManager().unregisterAll(plugin);
        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.16", pluginName), ex, plugin); // Paper
        }

        try {
            HandlerList.unregisterAll(plugin);
        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.17", pluginName), ex, plugin); // Paper
        }

        // Paper start - lifecycle event system
        try {
            io.papermc.paper.plugin.lifecycle.event.LifecycleEventRunner.INSTANCE.unregisterAllEventHandlersFor(plugin);
        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.18", pluginName), ex, plugin);
        }
        // Paper end

        try {
            this.server.getMessenger().unregisterIncomingPluginChannel(plugin);
            this.server.getMessenger().unregisterOutgoingPluginChannel(plugin);
        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.19", pluginName), ex, plugin); // Paper
        }

        try {
            for (World world : this.server.getWorlds()) {
                world.removePluginChunkTickets(plugin);
            }
        } catch (Throwable ex) {
            this.handlePluginException(I18n.as("paperplugininstancemanager.20", pluginName), ex, plugin); // Paper
        }

    }

    // TODO: Implement event part in future patch (paper patch move up, this patch is lower)
    private void handlePluginException(String msg, Throwable ex, Plugin plugin) {
        Bukkit.getServer().getLogger().log(Level.SEVERE, msg, ex);
        //this.pluginManager.callEvent(new com.destroystokyo.paper.event.server.ServerExceptionEvent(new com.destroystokyo.paper.exception.ServerPluginEnableDisableException(msg, ex, plugin)));
    }

    public boolean isTransitiveDepend(@NotNull PluginMeta plugin, @NotNull PluginMeta depend) {
        return this.dependencyTree.isTransitiveDependency(plugin, depend);
    }

    public boolean hasDependency(String pluginIdentifier) {
        return this.getPlugin(pluginIdentifier) != null;
    }

    // Debug only
    @ApiStatus.Internal
    public MutableGraph<String> getDependencyGraph() {
        return this.dependencyTree.getGraph();
    }
}
