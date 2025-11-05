package com.mohistmc.youer.plugins.ban;

import com.mohistmc.youer.plugins.config.YouerPluginConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BanConfig extends YouerPluginConfig {

    public static final String PARENT = "youer-config/bans";
    public static BanConfig MOSHOU;
    public static BanConfig ITEM;
    public static BanConfig ENTITY;
    public static BanConfig ENCHANTMENT;
    public static BanConfig BAN_MESSAGE;
    public static BanConfig RECIPE;
    public static BanConfig BLOCK;

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
    }

    public void addMoShou(String name) {
        if (!has("ITEMS")) {
            put("ITEMS", List.of());
        }
        List<String> list = MOSHOU.yaml.getStringList("ITEMS");
        list.add(name);
        put("ITEMS", list);
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

    public String getMessage(String name) {
        return (!has(name)) ? "" : BAN_MESSAGE.yaml.getString(name, "");
    }

    public void setBaMoShou(List<String> v) {
        MOSHOU.yaml.set("ITEMS", v);
        save();
    }

    public void setBanMessage(String key, Object v) {
        BAN_MESSAGE.yaml.set(key, v);
        save();
    }
}
