package com.mohistmc.youer.feature;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.util.I18n;
import com.mohistmc.youer.util.YamlUtils;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.NamedThreadFactory;
import net.minecraft.world.entity.Mob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/25 23:56:03
 */
public class EntityClear {

    public static final Logger LOGGER = LogManager.getLogger("EntityClear");
    // 清理前的预告时间（秒）
    public static final long WARN_TIME = 30;

    public static final ScheduledExecutorService ENTITYCLEAR_ITEM = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("EntityClear - Item"));
    public static final ScheduledExecutorService ENTITYCLEAR_MONSTER = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("EntityClear - Monster"));

    public static void start() {
        if (YouerConfig.clear_item) {
            ENTITYCLEAR_ITEM.scheduleAtFixedRate(() -> {
                if (MinecraftServer.getServer().hasStopped()) {
                    return;
                }
                warn_item();
                ENTITYCLEAR_ITEM.schedule(() -> {
                    if (MinecraftServer.getServer().hasStopped()) {
                        return;
                    }
                    run_item();
                }, WARN_TIME, TimeUnit.SECONDS);
            }, 1000 * 60, 1000L * YouerConfig.clear_item_time, TimeUnit.MILLISECONDS);
        }
        if (YouerConfig.clear_monster) {
            ENTITYCLEAR_MONSTER.scheduleAtFixedRate(() -> {
                if (MinecraftServer.getServer().hasStopped()) {
                    return;
                }
                warn_monster();
                ENTITYCLEAR_MONSTER.schedule(() -> {
                    if (MinecraftServer.getServer().hasStopped()) {
                        return;
                    }
                    run_monster();
                }, WARN_TIME, TimeUnit.SECONDS);
            }, 1000 * 60, 1000L * YouerConfig.clear_monster_time, TimeUnit.MILLISECONDS);
        }
    }

    public static void stop() {
        ENTITYCLEAR_ITEM.shutdown();
        ENTITYCLEAR_MONSTER.shutdown();
    }

    public static void warn_item() {
        for (long seconds = WARN_TIME; seconds > 0; seconds--) {
            final long s = seconds;
            ENTITYCLEAR_ITEM.schedule(() -> {
                if (MinecraftServer.getServer().hasStopped()) {
                    return;
                }
                MinecraftServer.getServer().execute(() -> {
                    String msg = (s <= 5 ? "§c" : "") + I18n.as("entityclear.item.warn", s);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(msg);
                    }
                });
            }, WARN_TIME - s, TimeUnit.SECONDS);
        }
    }

    public static void warn_monster() {
        for (long seconds = WARN_TIME; seconds > 0; seconds--) {
            final long s = seconds;
            ENTITYCLEAR_MONSTER.schedule(() -> {
                if (MinecraftServer.getServer().hasStopped()) {
                    return;
                }
                MinecraftServer.getServer().execute(() -> {
                    String msg = (s <= 5 ? "§c" : "") + I18n.as("entityclear.monster.warn", s);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(msg);
                    }
                });
            }, WARN_TIME - s, TimeUnit.SECONDS);
        }
    }

    public static List<String> getWhitelist(boolean item) {
        return item ? YouerConfig.clear_item_whitelist : YouerConfig.clear_monster_whitelist;
    }

    public static void saveItemWhitelist(List<String> list) {
        YouerConfig.yml.set("entity.clear.item.whitelist", list);
        YamlUtils.save(YouerConfig.youeryml, YouerConfig.yml);
        YouerConfig.clear_item_whitelist = list;
    }

    public static void saveMonsterWhitelist(List<String> list) {
        YouerConfig.yml.set("entity.clear.monster.whitelist", list);
        YamlUtils.save(YouerConfig.youeryml, YouerConfig.yml);
        YouerConfig.clear_monster_whitelist = list;
    }

    public static boolean shouldSkipItem(ItemStack itemStack, List<String> whitelist) {
        if (whitelist.contains(itemStack.getType().getKey().asString())) {
            return true;
        }
        if (whitelist.contains(itemStack.getType().name())) {
            return true;
        }
        return itemStack.hasCustomModelData() || !itemStack.getPersistentDataContainer().isEmpty();
    }


    public static boolean shouldSkipEntity(Mob entity, List<String> whitelist) {
        if (!entity.isAlive()) {
            return true;
        }
        if (whitelist.contains(entity.getBukkitEntity().getType().getKey().asString())) {
            return true;
        }
        if (entity.hasCustomName()) {
            return true;
        }
        return entity.isPersistenceRequired() || entity.requiresCustomPersistence() || !entity.getBukkitEntity().getPersistentDataContainer().isEmpty();
    }

    public static void run_item() {
        MinecraftServer.getServer().execute(() -> {
            long start = System.nanoTime();
            int size_stack = 0;
            long size_item = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item item && !shouldSkipItem(item.getItemStack(), YouerConfig.clear_item_whitelist)) {
                        size_item += item.getItemStack().getAmount();
                        entity.remove();
                        size_stack++;
                    }
                }
            }
            String msg = I18n.as("entityclear.item.done", size_stack, size_item, String.format("%.2f", (System.nanoTime() - start) / 1_000_000.0));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(msg);
            }
        });
    }

    public static void run_monster() {
        MinecraftServer.getServer().execute(() -> {
            long start = System.nanoTime();
            int size_monster = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (((CraftEntity)entity).getHandle() instanceof Mob mob && !shouldSkipEntity(mob, YouerConfig.clear_monster_whitelist)) {
                        entity.remove();
                        size_monster++;
                    }
                }
            }
            String msg = I18n.as("entityclear.monster.done", size_monster, String.format("%.2f", (System.nanoTime() - start) / 1_000_000.0));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(msg);
            }
        });
    }
}
