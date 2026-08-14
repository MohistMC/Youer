package com.mohistmc.youer.feature.entityclear;

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

    // 非 final — reload 时需要重建调度器以应用新的 enable/time 配置
    public static ScheduledExecutorService ENTITYCLEAR_ITEM;
    public static ScheduledExecutorService ENTITYCLEAR_MONSTER;

    public static void start() {
        restartSchedulers();
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
        shutdownSchedulers();
    }

    /** 重建 item/monster 调度器 — 使 reload 后新的 enable/time 配置生效 */
    private static void restartSchedulers() {
        shutdownSchedulers();
        ENTITYCLEAR_ITEM = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("EntityClear - Item"));
        ENTITYCLEAR_MONSTER = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("EntityClear - Monster"));
    }

    private static void shutdownSchedulers() {
        if (ENTITYCLEAR_ITEM != null) {
            ENTITYCLEAR_ITEM.shutdownNow();
            ENTITYCLEAR_ITEM = null;
        }
        if (ENTITYCLEAR_MONSTER != null) {
            ENTITYCLEAR_MONSTER.shutdownNow();
            ENTITYCLEAR_MONSTER = null;
        }
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
                    String msg = (s <= 5 ? "§c" : "") + I18n.as("entityclear.entity.warn", s);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(msg);
                    }
                });
            }, WARN_TIME - s, TimeUnit.SECONDS);
        }
    }

    // ==================== 模式与列表判断 ====================

    /**
     * 当前模式是否为白名单（默认）— 由 entity.clear.item.mode / entity.clear.monster.mode 分别配置，
     * blacklist 为黑名单模式。
     *
     * @param item true 使用物品模式，false 使用实体模式
     */
    public static boolean isWhitelistMode(boolean item) {
        String mode = item ? YouerConfig.clear_item_mode : YouerConfig.clear_monster_mode;
        return !"blacklist".equalsIgnoreCase(mode);
    }

    /**
     * 匹配列表条目，返回判定结果。
     * 返回 true  = 强制清理（应清除）
     * 返回 false = 排除清理（应保护，跳过）
     * 返回 null  = 未命中列表
     * <p>
     * 模式区别：
     * - 白名单模式（默认）：普通条目 → 排除清理；! 前缀条目 → 强制清理
     * - 黑名单模式：普通条目 → 强制清理；! 前缀条目 → 排除清理
     */
    private static Boolean matchItem(String itemKey, ItemStack itemStack, List<String> list) {
        for (String entry : list) {
            boolean inverted = entry.startsWith("!");
            String pattern = inverted ? entry.substring(1) : entry;
            boolean hit;
            if (pattern.endsWith(":*")) {
                // Wildcard match (modid:*) - any item whose key starts with the modid prefix
                hit = itemKey.startsWith(pattern.substring(0, pattern.length() - 1));
            } else {
                hit = pattern.equals(itemKey) || pattern.equals(itemStack.getType().name());
            }
            if (hit) {
                return isWhitelistMode(true) ? inverted : !inverted;
            }
        }
        return null;
    }

    private static Boolean matchEntity(String entityKey, List<String> list) {
        for (String entry : list) {
            boolean inverted = entry.startsWith("!");
            String pattern = inverted ? entry.substring(1) : entry;
            boolean hit;
            if (pattern.endsWith(":*")) {
                // Wildcard match (modid:*)
                hit = entityKey.startsWith(pattern.substring(0, pattern.length() - 1));
            } else {
                hit = pattern.equals(entityKey);
            }
            if (hit) {
                return isWhitelistMode(false) ? inverted : !inverted;
            }
        }
        return null;
    }

    public static boolean shouldSkipItem(ItemStack itemStack, List<String> list) {
        String itemKey = itemStack.getType().getKey().asString();
        Boolean match = matchItem(itemKey, itemStack, list);
        if (match != null) {
            return !match; // 强制清理 → false（不跳过）；排除清理 → true（跳过）
        }
        return itemStack.hasCustomModelData() || !itemStack.getPersistentDataContainer().isEmpty();
    }

    public static boolean shouldSkipEntity(Mob entity, List<String> list) {
        if (!entity.isAlive()) {
            return true;
        }
        String entityKey = entity.getBukkitEntity().getType().getKey().asString();
        Boolean match = matchEntity(entityKey, list);
        if (match != null) {
            return !match;
        }
        if (entity.hasCustomName()) {
            return true;
        }
        return entity.isPersistenceRequired() || entity.requiresCustomPersistence() || !entity.getBukkitEntity().getPersistentDataContainer().isEmpty();
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

    // ==================== 清理执行 ====================

    public static void run_item() {
        MinecraftServer.getServer().execute(() -> {
            long start = System.nanoTime();
            int size_stack = 0;
            long size_item = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item item && !shouldSkipItem(item.getItemStack(), YouerConfig.clear_item_whitelist)) {
                        size_item += item.getItemStack().getAmount();
                        if (YouerConfig.trash_enable) {
                            EntityClearTrash.addItem(item.getItemStack());
                        }
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
                    if (((CraftEntity) entity).getHandle() instanceof Mob mob) {
                        if (!shouldSkipEntity(mob, YouerConfig.clear_monster_whitelist)) {
                            entity.remove();
                            size_monster++;
                        }
                    } else {
                        // 非 Mob 实体（箭、掉落物等）：仅响应强制清理条目
                        String entityKey = entity.getType().getKey().asString();
                        Boolean match = matchEntity(entityKey, YouerConfig.clear_monster_whitelist);
                        if (match != null && match) {
                            entity.remove();
                            size_monster++;
                        }
                    }
                }
            }
            String msg = I18n.as("entityclear.entity.done", size_monster, String.format("%.2f", (System.nanoTime() - start) / 1_000_000.0));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(msg);
            }
        });
    }
}
