package com.mohistmc.youer.feature.ban;

import com.mohistmc.youer.feature.db.BanDatabaseStorage;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.HumanEntity;

/**
 * @author Mgazul by MohistMC
 * <p>
 * Ban configuration storage backed by database — 8 independent tables.
 * Each can be independently set to sqlite/mysql via database.yml features.bans.<type>.
 * All list tables have a message column for custom ban messages.
 */
public class BanConfig {

    public static BanConfig ITEM_MOSHOU;
    public static BanConfig ITEM;
    public static BanConfig ENTITY;
    public static BanConfig ENCHANTMENT;
    public static BanConfig RECIPE;
    public static BanConfig BLOCK;
    public static BanConfig NBT;
    public static BanConfig WORLD;
    public static BanConfig STRUCTURE;
    public static BanConfig EFFECT;

    private static BanDatabaseStorage storage;
    private static final Map<BanType, List<String>> globalCache = new HashMap<>();
    private static final Map<BanType, BanConfig> typeToConfigMap = new HashMap<>();

    private final BanType banType;

    public BanConfig(BanType banType) {
        this.banType = banType;
    }

    public static void init() {
        storage = new BanDatabaseStorage();
        storage.init();

        ITEM_MOSHOU = new BanConfig(BanType.ITEM_MOSHOU);
        ITEM = new BanConfig(BanType.ITEM);
        ENTITY = new BanConfig(BanType.ENTITY);
        ENCHANTMENT = new BanConfig(BanType.ENCHANTMENT);
        RECIPE = new BanConfig(BanType.RECIPE);
        BLOCK = new BanConfig(BanType.BLOCK);
        NBT = new BanConfig(null); // NBT operations
        WORLD = new BanConfig(BanType.WORLD);
        STRUCTURE = new BanConfig(BanType.STRUCTURE);
        EFFECT = new BanConfig(BanType.EFFECT);

        typeToConfigMap.put(BanType.ITEM_MOSHOU, ITEM_MOSHOU);
        typeToConfigMap.put(BanType.ITEM, ITEM);
        typeToConfigMap.put(BanType.ENTITY, ENTITY);
        typeToConfigMap.put(BanType.ENCHANTMENT, ENCHANTMENT);
        typeToConfigMap.put(BanType.RECIPE, RECIPE);
        typeToConfigMap.put(BanType.BLOCK, BLOCK);
        typeToConfigMap.put(BanType.WORLD, WORLD);
        typeToConfigMap.put(BanType.STRUCTURE, STRUCTURE);
        typeToConfigMap.put(BanType.EFFECT, EFFECT);

        for (BanType type : typeToConfigMap.keySet()) refreshCache(type);
    }

    public static List<String> getListByType(BanType type) {
        return globalCache.getOrDefault(type, new ArrayList<>());
    }

    public static void refreshCache(BanType type) {
        List<String> list = storage.getList(type);
        globalCache.put(type, list != null ? new ArrayList<>(list) : new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public void put(String key, Object v) {
        if (banType != null && v instanceof List<?>) {
            storage.setList(banType, (List<String>) v);
            refreshCache(banType);
        }
    }

    public boolean has(String value) {
        if (this == NBT) return storage.hasNbtKey(value);
        if (banType == null) return false;
        return storage.has(banType, value);
    }

    public String getMessage(String key) {
        if (banType == null) {
            return "";
        }
        return storage.getMessage(banType, key);
    }

    public void setMessage(String key, String message) {
        if (banType == null) {
            return;
        }
        storage.setMessage(banType, key, message);
    }

    // === NBT methods ===

    public Set<String> getAllNbtKeys() {
        return storage.getNbtKeys();
    }

    public List<String> getNbtList(String key) {
        return storage.hasNbtKey(key) ? storage.getNbtList(key) : new ArrayList<>();
    }

    public void addNbt(String key, String value) {
        storage.addNbt(key, value);
    }

    public void removeNbt(String key, String nbt) {
        storage.removeNbt(key, nbt);
    }

    public void clearNbt(String key) {
        storage.clearNbt(key);
    }

    /** @deprecated Database doesn't need file reload. */
    @Deprecated
    public void reload() {
        for (BanType type : typeToConfigMap.keySet()) refreshCache(type);
    }

    public static void saveToYaml(HumanEntity player, ClickType clickType, List<String> list, BanType banType) {
        switch (banType) {
            case ITEM -> BanConfig.ITEM.put(banType.key, list);
            case ENTITY -> BanConfig.ENTITY.put(banType.key, list);
            case ENCHANTMENT -> BanConfig.ENCHANTMENT.put(banType.key, list);
            case ITEM_MOSHOU -> BanConfig.ITEM_MOSHOU.put(banType.key, list);
            case RECIPE -> BanConfig.RECIPE.put(banType.key, list);
            case BLOCK -> BanConfig.BLOCK.put(banType.key, list);
            case WORLD -> BanConfig.WORLD.put(banType.key, list);
            case STRUCTURE -> BanConfig.STRUCTURE.put(banType.key, list);
            case EFFECT -> BanConfig.EFFECT.put(banType.key, list);
        }
        if (clickType == ClickType.ADD) {
            player.sendMessage(I18n.as(banType.i18n_key_add));
        } else if (clickType == ClickType.REMOVE) {
            player.sendMessage(I18n.as(banType.i18n_key_remove));
        }
    }
}
