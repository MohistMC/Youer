package com.mohistmc.youer.feature.world;

import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.api.WorldAPI;
import com.mohistmc.youer.util.YamlUtils;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class WorldConfig {
    public static File f = new File("youer-config", "worlds.yml");
    public static FileConfiguration config;

    /** Default entries for the per-world naturalspawn whitelist (worlds.yml). */
    public static final List<String> SPAWN_FOR_NATURAL_DEFAULT_WHITELIST = List.of(
            "minecraft:wandering_trader", "minecraft:phantom", "minecraft:warden");

    /**
     * Reads of {@code worlds.yml} never touch disk after the first load: the backing {@link #config}
     * is a single in-memory {@link FileConfiguration} loaded once (and on {@link #reload()}). The two
     * flags read on the spawn hot path are additionally cached per world so a spawn costs a
     * lock-free map lookup, not a synchronized configuration walk.
     */
    private static final ConcurrentHashMap<String, Boolean> naturalSpawnCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> chunkSpawnCache = new ConcurrentHashMap<>();

    /**
     * Writes are coalesced: every mutation just marks the file dirty and a single flush is scheduled
     * on the next server tick, collapsing the burst of saves that {@link #loadWorlds()} / addWorld
     * would otherwise perform. A shutdown hook guarantees the last dirty state still hits disk.
     */
    private static volatile boolean dirty = false;
    private static final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    static {
        reload();
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(WorldConfig::saveNow, "WorldConfig-flush"));
        } catch (Throwable ignored) {
        }
    }

    /** Reloads the in-memory config from disk and drops all per-world caches. */
    public static void reload() {
        config = YamlConfiguration.loadConfiguration(f);
        naturalSpawnCache.clear();
        chunkSpawnCache.clear();
    }

    /**
     * Schedules a single coalesced flush on the server's main thread next tick, collapsing the burst
     * of saves that startup/commands would otherwise perform. Falls back to an immediate write when
     * no server is up yet.
     */
    public static void markDirty() {
        dirty = true;
        if (flushScheduled.compareAndSet(false, true)) {
            try {
                MinecraftServer server = MinecraftServer.getServer();
                if (server != null) {
                    server.execute(WorldConfig::flush);
                    return;
                }
            } catch (Throwable ignored) {
            }
            flushScheduled.set(false);
            saveNow();
        }
    }

    /** Flush entry point used by the scheduled task. */
    public static void flush() {
        flushScheduled.set(false);
        if (dirty) {
            saveNow();
        }
    }

    /** Writes the in-memory config to disk immediately and clears the dirty flag. */
    public static void saveNow() {
        if (f == null || config == null) {
            return;
        }
        YamlUtils.save(f, config);
        dirty = false;
    }

    /** Persist the current in-memory config (coalesced). Kept for backward compatibility. */
    public static void init() {
        markDirty();
    }

    public static void addInfo(String w, String info) {
        World world = Bukkit.getWorld(w);
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".info", info);
            }
            init();
        }
    }

    public static void addname(String w, String info) {
        World world = Bukkit.getWorld(w);
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".name", info);
            }
            init();
        }
    }

    public static void setnandu(Player player, String nandu) {
        World world = player.getWorld();
        if (f.exists()) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".difficulty", nandu);
            }
            init();
        }
    }

    public static void setGameMode(World world, String nandu) {
        if (f.exists()) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".gamemode", nandu);
            }
            init();
        }
    }

    public static GameMode getGameMode(World world) {
        if (f.exists()) {
            if (config.getString("worlds." + world.getName()) != null) {
                if (config.get("worlds." + world.getName() + ".gamemode") != null) {
                    return GameMode.valueOf(config.getString("worlds." + world.getName() + ".gamemode"));
                }
            }
        }
        return null;
    }

    public static void saveWorldBorder(World world) {
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".worldborder", world.getWorldBorder().getSize());
                config.set("worlds." + world.getName() + ".worldborder_center_x", world.getWorldBorder().getCenter().getX());
                config.set("worlds." + world.getName() + ".worldborder_center_z", world.getWorldBorder().getCenter().getZ());
            }
            init();
        }
    }

    public static void saveWorldBorder(World world, double newSize) {
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".worldborder", newSize);
                config.set("worlds." + world.getName() + ".worldborder_center_x", world.getWorldBorder().getCenter().getX());
                config.set("worlds." + world.getName() + ".worldborder_center_z", world.getWorldBorder().getCenter().getZ());
            }
            init();
        }
    }

    public static void saveWorldBorderCenter(World world, double newCenterX, double newCenterZ) {
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".worldborder", world.getWorldBorder().getSize());
                config.set("worlds." + world.getName() + ".worldborder_center_x", newCenterX);
                config.set("worlds." + world.getName() + ".worldborder_center_z", newCenterZ);
            }
            init();
        }
    }

    public static void saveGameRule(World world, String ruleName, String value) {
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".gamerules." + ruleName, value);
            }
            init();
        }
    }

    public static void loadGameRules(World world) {
        if (f.exists() && world != null) {
            ConfigurationSection gamerules = config.getConfigurationSection("worlds." + world.getName() + ".gamerules");
            if (gamerules != null) {
                for (String key : gamerules.getKeys(false)) {
                    org.bukkit.GameRule<?> gameRule = org.bukkit.GameRule.getByName(key);
                    if (gameRule != null) {
                        String value = gamerules.getString(key);
                        if (value != null) {
                            applyGameRule(world, gameRule, value);
                        }
                    }
                }
            }
        }
    }

    private static <T> void applyGameRule(World world, org.bukkit.GameRule<T> gameRule, String value) {
        world.setGameRule(gameRule, parseGameRuleValue(gameRule, value));
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseGameRuleValue(org.bukkit.GameRule<T> gameRule, String value) {
        if (gameRule.getType() == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (gameRule.getType() == Integer.class) {
            return (T) Integer.valueOf(value);
        }
        return null;
    }

    public static void addWorld(String w, boolean isYouer) {
        if (Bukkit.getWorld(w) != null) {
            World world = Bukkit.getWorld(w);
            String world_name = world.getName();
            if (f.exists() && isYouer) {
                config.set("worlds." + world_name + ".youer", isYouer);
                if (config.getString("worlds." + world_name + ".info") == null) {
                    config.set("worlds." + world_name + ".seed", world.getSeed());
                    config.set("worlds." + world_name + ".environment", world.getEnvironment().name());
                    config.set("worlds." + world_name + ".name", world_name);
                    config.set("worlds." + world_name + ".info", "-/-");
                    config.set("worlds." + world_name + ".difficulty", world.getDifficulty().name());
                    config.set("worlds." + world_name + ".youer", isYouer);
                    config.set("worlds." + world_name + ".keepspawninmemory", true);
                }
                init();
            }
        }
    }

    public static void initMods(ServerLevel level) {
        CraftWorld world = level.getWorld();
        if (world.isMods()) {
            addWorld(world.getName(), false);
            config.set("worlds." + world.getName() + ".ismods", world.isMods());
            config.set("worlds." + world.getName() + ".modName", world.getModid());
            config.set("worlds." + world.getName() + ".keepspawninmemory", false);
        }
        init();

    }

    public static void loadWorlds() {
        ConfigurationSection section = config.getConfigurationSection("worlds");
        if (section != null) {
            for (String w : section.getKeys(false)) {
                boolean canload = true;
                if (Objects.equals(w, "DIM1")) {
                    if (!Bukkit.getAllowNether()) {
                        config.set("worlds." + w, null);
                        init();
                        canload = false;
                    }
                } else if (Objects.equals(w, "DIM-1")) {
                    if (!Bukkit.getAllowEnd()) {
                        config.set("worlds." + w, null);
                        init();
                        canload = false;
                    }
                }
                String environment = "NORMAL";
                boolean isMods = false;
                boolean isYouer = false;
                String modName = null;
                boolean keepspawninmemory = true;
                boolean isVoid = false;
                boolean isFlat = false;
                if (Bukkit.getWorld(w) == null) {
                    long seed = -1L;
                    if (config.get("worlds." + w + ".seed") != null) {
                        seed = config.getLong("worlds." + w + ".seed");
                    }
                    if (config.get("worlds." + w + ".environment") != null) {
                        environment = config.getString("worlds." + w + ".environment");
                    }
                    if (config.get("worlds." + w + ".ismods") != null) {
                        isMods = config.getBoolean("worlds." + w + ".ismods");
                    }
                    if (config.get("worlds." + w + ".modName") != null) {
                        modName = config.getString("worlds." + w + ".modName");
                    }
                    if (config.get("worlds." + w + ".youer") != null) {
                        isYouer = config.getBoolean("worlds." + w + ".youer");
                    }
                    if (config.get("worlds." + w + ".keepspawninmemory") != null) {
                        keepspawninmemory = config.getBoolean("worlds." + w + ".keepspawninmemory");
                    }
                    if (config.get("worlds." + w + ".void") != null) {
                        isVoid = config.getBoolean("worlds." + w + ".void");
                    }
                    if (config.get("worlds." + w + ".flat") != null) {
                        isFlat = config.getBoolean("worlds." + w + ".flat");
                    }
                    // Worlds created by mods are no longer loaded when the mod is unloaded
                    if (isMods && !ServerAPI.hasMod(modName)) {
                        config.set("worlds." + w, null);
                        init();
                        canload = false;
                    }
                    if (!isYouer) {
                        canload = false;
                    }
                    if (canload) {
                        WorldCreator wc = new WorldCreator(w);
                        if (isVoid) wc.generator(new WorldAPI.VoidGenerator());
                        if (isFlat) {
                            wc.type(WorldType.FLAT);
                            wc.generator(new WorldAPI.FlatGenerator());
                        }
                        wc.seed(seed);
                        wc.environment(World.Environment.valueOf(environment));
                        wc.keepSpawnInMemory(keepspawninmemory);
                        wc.createWorld();
                    }
                }
                World world = Bukkit.getWorld(w);
                if (world != null) {
                    world.setVoid(isVoid);
                    world.setFlat(isFlat);
                    if (config.get("worlds." + w + ".worldborder") != null) {
                        world.getWorldBorder().setSize(config.getDouble("worlds." + w + ".worldborder"));
                    }
                    if (config.get("worlds." + w + ".worldborder_center_x") != null && config.get("worlds." + w + ".worldborder_center_z") != null) {
                        world.getWorldBorder().setCenter(config.getDouble("worlds." + w + ".worldborder_center_x"), config.getDouble("worlds." + w + ".worldborder_center_z"));
                    }
                    loadGameRules(world);
                    config.set("worlds." + w + ".seed", world.getSeed());
                    init();
                    world.setKeepSpawnInMemory(config.getBoolean("worlds." + w + ".keepspawninmemory", true));
                    boolean natural = naturalSpawn(w);
                    world.setSpawnFlags(natural, natural);
                } else {
                    if (!isYouer && !isMods) {
                        config.set("worlds." + w, null);
                        init();
                    }

                }
            }
        }
    }

    public static void removeWorld(String w) {
        if (Bukkit.getWorld(w) != null) {
            World world = Bukkit.getWorld(w);
            if (f.exists() && world != null) {
                if (config.getString("worlds." + world.getName()) != null) {
                    config.set("worlds." + world.getName(), null);
                    init();
                }
            }
        }
    }

    public static void addSpawn(Location location) {
        World world = location.getWorld();
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName()) != null) {
                config.set("worlds." + world.getName() + ".spawn.x", location.getX());
                config.set("worlds." + world.getName() + ".spawn.y", location.getY());
                config.set("worlds." + world.getName() + ".spawn.z", location.getZ());
                config.set("worlds." + world.getName() + ".spawn.yaw", location.getYaw());
                config.set("worlds." + world.getName() + ".spawn.pitch", location.getPitch());
            }
            init();
        }
    }

    public static void getSpawn(String w, Player player) {
        World world = Bukkit.getWorld(w);
        if (f.exists() && world != null) {
            if (config.getString("worlds." + world.getName() + ".spawn") != null) {
                double x = config.getDouble("worlds." + world.getName() + ".spawn.x");
                double y = config.getDouble("worlds." + world.getName() + ".spawn.y");
                double z = config.getDouble("worlds." + world.getName() + ".spawn.z");
                double yaw = config.getDouble("worlds." + world.getName() + ".spawn.yaw");
                double pitch = config.getDouble("worlds." + world.getName() + ".spawn.pitch");
                player.teleport(new Location(world, x, y, z, (float) yaw, (float) pitch));
            } else {
                Location defaultSpawn = new Location(world, 0, world.getHighestBlockYAt(0, 0), 0);
                player.teleport(defaultSpawn, PlayerTeleportEvent.TeleportCause.YOUER);
            }
        }
    }

    public static void youer(String w, boolean isYouer) {
        config.set("worlds." + w + ".youer", isYouer);
        config.set("worlds." + w + ".keepspawninmemory", false);
        init();
    }

    public static void aVoid(String w, boolean isVoid) {
        config.set("worlds." + w + ".void", isVoid);
        init();
    }

    public static void aFlat(String w, boolean isVoid) {
        config.set("worlds." + w + ".flat", isVoid);
        init();
    }

    public static boolean keepspawninmemory(String w){
        return config.getBoolean("worlds." + w + ".keepspawninmemory", true);
    }

    public static void setKeepSpawnInMemory(String w, boolean keep) {
        config.set("worlds." + w + ".keepspawninmemory", keep);
        init();
    }

    public static boolean naturalSpawn(String w){
        return naturalSpawnCache.computeIfAbsent(w, k -> config.getBoolean("worlds." + k + ".naturalspawn", true));
    }

    public static void setNaturalSpawn(String w, boolean enabled) {
        config.set("worlds." + w + ".naturalspawn", enabled);
        naturalSpawnCache.put(w, enabled);
        init();
    }

    public static List<String> naturalSpawnWhitelist(String w) {
        List<String> list = config.getStringList("worlds." + w + ".naturalspawn-whitelist");
        return list.isEmpty() ? SPAWN_FOR_NATURAL_DEFAULT_WHITELIST : list;
    }

    public static void setNaturalSpawnWhitelist(String w, List<String> list) {
        config.set("worlds." + w + ".naturalspawn-whitelist", list);
        init();
    }

    public static boolean chunkSpawn(String w) {
        return chunkSpawnCache.computeIfAbsent(w, k -> config.getBoolean("worlds." + k + ".spawnforchunk", true));
    }

    public static void setChunkSpawn(String w, boolean enabled) {
        config.set("worlds." + w + ".spawnforchunk", enabled);
        chunkSpawnCache.put(w, enabled);
        init();
    }

    public static boolean isMaintenance(String w) {
        return f.exists() && config.getBoolean("worlds." + w + ".maintenance", false);
    }

    public static void setMaintenance(String w, boolean maintenance) {
        config.set("worlds." + w + ".maintenance", maintenance);
        init();
    }
}
