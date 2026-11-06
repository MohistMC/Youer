package com.mohistmc.youer.compat;

import com.mohistmc.youer.YouerConfig;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * packetevents looks entity types up in its own vanilla-only table, so the registry ids of modded entities
 * read back as null and its spawn-entity wrapper throws while re-encoding the packet, which drops the whole
 * connection. Handing those unknown ids to packetevents keeps third-party packet listeners usable.
 */
public final class PacketEventsCompat {

    private static final Logger LOGGER = LogManager.getLogger("Youer");
    private static final String PLUGIN_NAME = "packetevents";
    private static final String API = "com.github.retrooper.packetevents.";

    private static final Set<String> loggedTypes = ConcurrentHashMap.newKeySet();
    private static volatile boolean applied;

    private PacketEventsCompat() {
    }

    public static void onPluginEnabled(Plugin plugin) {
        if (PLUGIN_NAME.equalsIgnoreCase(plugin.getName())) {
            apply();
        }
    }

    public static void onServerLoad() {
        apply();
    }

    private static synchronized void apply() {
        if (applied || !YouerConfig.compat_packetevents_entity_types) {
            return;
        }
        Plugin plugin = findPlugin();
        if (plugin == null) {
            return;
        }
        try {
            int registered = registerUnknownEntityTypes(plugin.getClass().getClassLoader());
            if (registered < 0) {
                return;
            }
            applied = true;
            LOGGER.info("Registered {} entity type ids into packetevents so it can resolve modded entities", registered);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.warn("packetevents entity type compat skipped, modded spawn packets may still drop connections: {}", e.toString());
        }
    }

    private static Plugin findPlugin() {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (PLUGIN_NAME.equalsIgnoreCase(plugin.getName()) && plugin.isEnabled()) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * The registry is not extended through its {@code define} methods: they rebuild their id array from
     * mapping data packetevents discards while loading, so the version tables get filled directly instead.
     * A negative count means packetevents has not settled its server version yet and this should be retried.
     */
    private static int registerUnknownEntityTypes(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> clientVersionClass = loader.loadClass(API + "protocol.player.ClientVersion");
        Class<?> mappedEntityClass = loader.loadClass(API + "protocol.mapper.MappedEntity");
        Class<?> builderClass = loader.loadClass(API + "util.mappings.TypesBuilder");
        Class<?> dataClass = loader.loadClass(API + "util.mappings.TypesBuilderData");
        Class<?> mappingHelperClass = loader.loadClass(API + "util.mappings.MappingHelper");
        Class<?> nameClass = loader.loadClass(API + "resources.ResourceLocation");
        Class<?> rangeClass = loader.loadClass(API + "util.VersionRange");
        Class<?> typeClass = loader.loadClass(API + "protocol.entity.type.EntityType");
        Class<?> staticTypeClass = loader.loadClass(API + "protocol.entity.type.StaticEntityType");

        Constructor<?> nameConstructor = nameClass.getConstructor(String.class);
        Constructor<?> rangeConstructor = rangeClass.getConstructor(clientVersionClass, clientVersionClass);
        Constructor<?> dataConstructor = dataClass.getConstructor(nameClass, int[].class, builderClass, rangeClass);
        Constructor<?> staticTypeConstructor = staticTypeClass.getConstructor(dataClass, typeClass);
        Method registerMapping = mappingHelperClass.getMethod("registerMapping",
            builderClass, Map[].class, Map[].class, dataClass, mappedEntityClass);

        Object registry = loader.loadClass(API + "protocol.entity.type.EntityTypes").getMethod("getRegistry").invoke(null);
        Class<?> registryClass = registry.getClass();
        Object builder = registryClass.getMethod("getTypesBuilder").invoke(registry);
        Method getDataIndex = builderClass.getMethod("getDataIndex", clientVersionClass);
        Method getVersions = builderClass.getMethod("getVersions");
        Method getById = registryClass.getMethod("getById", clientVersionClass, int.class);
        Method getByName = registryClass.getMethod("getByName", clientVersionClass, String.class);

        Object serverVersion = serverClientVersion(loader, clientVersionClass);
        if (serverVersion == null) {
            return -1;
        }
        Object[] typeNames = tables(registry, "typeNames");
        Object[] typeIds = tables(registry, "typeIds");
        int index = (int) getDataIndex.invoke(builder, serverVersion);
        Object range = rangeConstructor.newInstance(serverVersion, serverVersion);
        int[] ids = new int[Array.getLength(getVersions.invoke(builder))];
        Arrays.fill(ids, -1);

        int registered = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            int id = BuiltInRegistries.ENTITY_TYPE.getId(type);
            if (key == null || id < 0 || known(registry, getById, getByName, serverVersion, id, key.toString())) {
                continue;
            }
            ids[index] = id;
            Object data = dataConstructor.newInstance(nameConstructor.newInstance(key.toString()), ids.clone(), builder, range);
            registerMapping.invoke(null, builder, typeNames, typeIds, data, staticTypeConstructor.newInstance(data, null));
            registered++;
        }
        return registered;
    }

    private static boolean known(Object registry, Method getById, Method getByName, Object serverVersion, int id, String key) {
        try {
            return getById.invoke(registry, serverVersion, id) != null || getByName.invoke(registry, serverVersion, key) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static Object[] tables(Object registry, String name) throws ReflectiveOperationException {
        Field field = registry.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (Object[]) field.get(registry);
    }

    /** packetevents encodes spawn packets against the server version, so that is the table to fill. */
    private static Object serverClientVersion(ClassLoader loader, Class<?> clientVersionClass) {
        try {
            Object api = loader.loadClass(API + "PacketEvents").getMethod("getAPI").invoke(null);
            Object serverManager = loader.loadClass(API + "PacketEventsAPI").getMethod("getServerManager").invoke(api);
            Object serverVersion = loader.loadClass(API + "manager.server.ServerManager").getMethod("getVersion").invoke(serverManager);
            return loader.loadClass(API + "manager.server.ServerVersion").getMethod("toClientVersion").invoke(serverVersion);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("packetevents server version unknown, entity type compat skipped: {}", e.toString());
            return null;
        }
    }

    public static void logSpawnPacket(Entity entity) {
        if (!YouerConfig.compat_log_entity_type_ids) {
            return;
        }
        EntityType<?> type = entity.getType();
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key == null || !loggedTypes.add(key + " id=" + BuiltInRegistries.ENTITY_TYPE.getId(type))) {
            return;
        }
        LOGGER.info("Spawn packet sent for {} (registry id {}) - {}", key, BuiltInRegistries.ENTITY_TYPE.getId(type),
            "minecraft".equals(key.getNamespace()) ? "vanilla, known to packetevents" : "modded, needs packetevents compat");
    }
}
