package com.mohistmc.youer.api.gui;

import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.feature.GlobalVariableSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * @author LSeng
 */
public class ItemStackFactory {

    ItemStack item;
    Player player;

    public ItemStackFactory(String typeOrKey) {
        this(Material.matchMaterial(typeOrKey), 1);
    }

    public ItemStackFactory(Material type) {
        this(type, 1);
    }

    public ItemStackFactory(String typeOrKey, int amount) {
        this(Material.matchMaterial(typeOrKey), amount);
    }

    public ItemStackFactory(Material type, int amount) {
        this.item = new ItemStack(type, amount);
    }

    public ItemStackFactory(ItemStack item) {
        this.item = item.clone();
    }

    public ItemStack build() {
        return this.item;
    }

    public ItemStackFactory setDisplayName(String name) {
        ItemMeta im = this.item.getItemMeta();
        im.setDisplayName(name);
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory setEnchantment(Enchantment enchantment) {
        ItemMeta im = this.item.getItemMeta();
        if (enchantment != null) {
            im.addEnchant(enchantment, 1, false);
        }
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory setEnchantment(Enchantment enchantment, int level) {
        ItemMeta im = this.item.getItemMeta();
        if (enchantment != null) {
            im.addEnchant(enchantment, level, false);
        }
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory setLore(List<String> lores) {
        ItemMeta im = this.item.getItemMeta();
        List<String> lores_ = lores.stream().map(this::hook).collect(Collectors.toList());
        im.setLore(lores_);
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory addLore(String lore) {
        ItemMeta im = this.item.getItemMeta();
        List<String> lores = im.getLore();
        if (lores == null) {
            lores = new ArrayList<>();
        }
        lores.add(hook(lore));
        im.setLore(lores);
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory customModelData(int customModelData) {
        ItemMeta im = this.item.getItemMeta();
        im.setCustomModelData(customModelData);
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory hideTooltip() {
        ItemMeta im = this.item.getItemMeta();
        im.setHideTooltip(true);
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory player(Player player) {
        this.player = player;
        return this;
    }

    public ItemStackFactory setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemStackFactory setDurability(short durability) {
        item.setDurability(durability);
        return this;
    }

    public ItemStackFactory addItemFlags(ItemFlag... itemFlags) {
        ItemMeta im = this.item.getItemMeta();
        im.addItemFlags(itemFlags);
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory addItemRarity(ItemRarity rarity) {
        ItemMeta im = this.item.getItemMeta();
        im.setRarity(rarity);
        this.item.setItemMeta(im);
        return this;
    }

    public ItemStackFactory head(String base64) {
        SkullMeta meta = (SkullMeta) this.item.getItemMeta();
        if (player != null) {
            meta.setDisplayName(player.getName());
            ItemAPI.setSkullTexture(meta, base64);
            this.item.setItemMeta(meta);
        }

        return this;
    }

    public boolean isSkull() {
        return item.getType() == Material.PLAYER_HEAD;
    }

    private String hook(String text) {
        if (player != null) {
            return GlobalVariableSystem.as(player, text);
        }
        return text;
    }

}
