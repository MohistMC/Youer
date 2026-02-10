package com.mohistmc.youer.feature.ban;

import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    }

    public List<String> getMoShouList() {
        return (!has("ITEMS")) ? new ArrayList<>() : MOSHOU.yaml.getStringList("ITEMS");
    }

    public List<String> getItem() {
        return (!has("ITEMS")) ? new ArrayList<>() : ITEM.yaml.getStringList("ITEMS");
    }

    public List<String> getEntity() {
        return (!has("ENTITYS")) ? new ArrayList<>() : ENTITY.yaml.getStringList("ENTITYS");
    }

    public List<String> getEnchantment() {
        return (!has("ENCHANTMENTS")) ? new ArrayList<>() : ENCHANTMENT.yaml.getStringList("ENCHANTMENTS");
    }

    public List<String> getRecipe() {
        return (!has("RECIPES")) ? new ArrayList<>() : RECIPE.yaml.getStringList("RECIPES");
    }

    public List<String> getBlock() {
        return (!has("BLOCKS")) ? new ArrayList<>() : BLOCK.yaml.getStringList("BLOCKS");
    }

    public List<String> getWorld() {
        return (!has("WORLDS")) ? new ArrayList<>() : WORLD.yaml.getStringList("WORLDS");
    }

    public String getMessage(String name) {
        return (!has(name)) ? "" : BAN_MESSAGE.yaml.getString(name, "");
    }

    public void setBanMessage(String key, Object v) {
        BAN_MESSAGE.yaml.set(key, v);
        save();
    }

    public Set<String> getAllNbtKeys() {
        return NBT.yaml.getKeys(false);
    }

    public List<String> getNbtList(String key) {
        return (!NBT.has(key)) ? new ArrayList<>() : NBT.yaml.getStringList(key);
    }

    public void addNbt(String key, String v) {
        var list = NBT.yaml.getStringList(key);
        list.add(v);
        NBT.yaml.set(key, list);
        save();
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
