package com.mohistmc.youer.feature.ban;

import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.InvalidConfigurationException;

public class BanConfig extends YouerPluginConfig {

    public static final String PARENT = "youer-config/bans";
    public static BanConfig MOSHOU;
    public static BanConfig ITEM;
    public static BanConfig ENTITY;
    public static BanConfig ENCHANTMENT;
    public static BanConfig BAN_MESSAGE;
    public static BanConfig RECIPE;
    public static BanConfig BLOCK;
    public static BanConfig NBT;
    public static BanConfig WORLD;
    public static BanConfig STRUCTURE;

    private static final Map<BanType, List<String>> globalCache = new HashMap<>();
    private static final Map<BanType, BanConfig> typeToConfigMap = new HashMap<>();

    public BanConfig(File file) {
        super(file);
    }

    public static void init() {
        MOSHOU = new BanConfig(new File(PARENT, "item-moshou.yml"));
        ITEM = new BanConfig(new File(PARENT, "item.yml"));
        ENTITY = new BanConfig(new File(PARENT, "entity.yml"));
        ENCHANTMENT = new BanConfig(new File(PARENT, "enchantment.yml"));
        BAN_MESSAGE = new BanConfig(new File(PARENT, "item-message.yml"));
        RECIPE = new BanConfig(new File(PARENT, "recipe.yml"));
        BLOCK = new BanConfig(new File(PARENT, "block.yml"));
        NBT = new BanConfig(new File(PARENT, "nbt.yml"));
        WORLD = new BanConfig(new File(PARENT, "world.yml"));
        STRUCTURE = new BanConfig(new File(PARENT, "structure.yml"));

        typeToConfigMap.put(BanType.ITEM_MOSHOU, MOSHOU);
        typeToConfigMap.put(BanType.ITEM, ITEM);
        typeToConfigMap.put(BanType.ENTITY, ENTITY);
        typeToConfigMap.put(BanType.ENCHANTMENT, ENCHANTMENT);
        typeToConfigMap.put(BanType.RECIPE, RECIPE);
        typeToConfigMap.put(BanType.BLOCK, BLOCK);
        typeToConfigMap.put(BanType.WORLD, WORLD);
        typeToConfigMap.put(BanType.STRUCTURE, STRUCTURE);

        refreshCache(BanType.ITEM_MOSHOU);
        refreshCache(BanType.ITEM);
        refreshCache(BanType.ENTITY);
        refreshCache(BanType.ENCHANTMENT);
        refreshCache(BanType.RECIPE);
        refreshCache(BanType.BLOCK);
        refreshCache(BanType.WORLD);
        refreshCache(BanType.STRUCTURE);
    }

    public static List<String> getListByType(BanType type) {
        return globalCache.getOrDefault(type, new ArrayList<>());
    }

    public List<String> getList(BanType type) {
        return getListByType(type);
    }

    @Override
    public void put(String key, Object v) {
        yaml.set(key, v);
        save();
        refreshCacheByKey(key);
    }

    public static void refreshCache(BanType type) {
        BanConfig config = typeToConfigMap.get(type);
        if (config != null) {
            try {
                List<String> list = config.yaml.getStringList(type.key);
                globalCache.put(type, list != null ? new ArrayList<>(list) : new ArrayList<>());
            } catch (Exception e) {
                globalCache.put(type, new ArrayList<>());
            }
        }
    }

    private void refreshCacheByKey(String key) {
        for (Map.Entry<BanType, BanConfig> entry : typeToConfigMap.entrySet()) {
            if (entry.getValue() == this && entry.getKey().key.equals(key)) {
                refreshCache(entry.getKey());
                break;
            }
        }
    }

    public void reload() throws IOException, InvalidConfigurationException {
        this.yaml.load(this.config);
        for (Map.Entry<BanType, BanConfig> entry : typeToConfigMap.entrySet()) {
            if (entry.getValue() == this) {
                refreshCache(entry.getKey());
            }
        }
    }

    public static void reloadAll() {
        for (BanConfig config : new BanConfig[]{MOSHOU, ITEM, ENTITY, ENCHANTMENT, RECIPE, BLOCK, NBT, WORLD, STRUCTURE, BAN_MESSAGE}) {
            try {
                config.yaml.load(config.config);
                for (Map.Entry<BanType, BanConfig> entry : typeToConfigMap.entrySet()) {
                    if (entry.getValue() == config) {
                        refreshCache(entry.getKey());
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void setBanMessage(String key, Object value) {
        BAN_MESSAGE.yaml.set(key, value);
        BAN_MESSAGE.save();
    }

    public String getMessage(String key) {
        return BAN_MESSAGE.yaml.getString(key, "");
    }

    public Set<String> getAllNbtKeys() {
        return NBT.yaml.getKeys(false);
    }

    public List<String> getNbtList(String key) {
        return (!NBT.has(key)) ? new ArrayList<>() : NBT.yaml.getStringList(key);
    }

    public void addNbt(String key, String value) {
        var list = NBT.yaml.getStringList(key);
        list.add(value);
        NBT.yaml.set(key, list);
        NBT.save();
    }

    public void removeNbt(String key, String nbt) {
        if (!NBT.has(key)) return;

        var list = NBT.yaml.getStringList(key);
        list.remove(nbt);
        if (list.isEmpty()) {
            NBT.yaml.set(key, null);
        } else {
            NBT.yaml.set(key, list);
        }
        NBT.save();
    }

    public void clearNbt(String key) {
        if (NBT.has(key)) {
            NBT.yaml.set(key, null);
            NBT.save();
        }
    }
}
