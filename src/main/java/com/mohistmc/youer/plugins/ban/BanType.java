package com.mohistmc.youer.plugins.ban;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 15:10:13
 */
public enum BanType {

    ITEM("ITEMS", "bans.add.item", "bans.remove.item"),
    ITEM_MOSHOU("ITEMS", "bans.add.item_moshou", "bans.remove.item_moshou"),
    ENTITY("ENTITYS", "bans.add.entity", "bans.remove.entity"),
    ENCHANTMENT("ENCHANTMENTS", "bans.add.enchantment", "bans.remove.enchantment"),
    RECIPE("RECIPES", "bans.add.recipe", "bans.remove.recipe"),
    BLOCK("BLOCKS", "bans.add.block", "bans.remove.block");

    public final String key;
    public final String i18n_key_add;
    public final String i18n_key_remove;

    BanType(String key, String i18n_key_add, String i18n_key_remove) {
        this.key = key;
        this.i18n_key_add = i18n_key_add;
        this.i18n_key_remove = i18n_key_remove;
    }
}
