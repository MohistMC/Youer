package com.mohistmc.youer.api;

import com.mohistmc.mjson.Json;
import com.mohistmc.tools.Base64Utils;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import java.net.URI;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SpawnEggItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemAPI {

    public static final Logger LOGGER = LogManager.getLogger("ItemAPI");

    public static net.minecraft.world.item.ItemStack toNMSItem(Material material) {
        ItemStack itemStack = new ItemStack(material);
        return CraftItemStack.asNMSCopy(itemStack);
    }

    /**
     * Parse Base64 into {@link org.bukkit.inventory.ItemStack}
     *
     * @param base64
     * @return
     */
    public static ItemStack base64(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(data);
        } catch (Exception e) {
            LOGGER.error("Unable to decode class type.");
            return Material.AIR.buildItem();
        }
    }

    /**
     * Parse {@link org.bukkit.inventory.ItemStack} into Base64
     *
     * @param stack
     * @return
     */
    public static String base64(ItemStack stack) {
        try {
            byte[] data = stack.serializeAsBytes();
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            return null;
        }
    }

    public static void name(ItemStack itemStack, String name) {
        ItemMeta im = itemStack.getItemMeta();
        im.displayName(ColorAPI.adventure(name));
        itemStack.setItemMeta(im);
    }

    public static void rarity(ItemStack itemStack, ItemRarity rarity) {
        ItemMeta im = itemStack.getItemMeta();
        im.setRarity(rarity);
        itemStack.setItemMeta(im);
    }

    public static void lore(ItemStack itemStack, List<String> lores) {
        ItemMeta im = itemStack.getItemMeta();
        List<Component> lores_ = lores.stream().map(ColorAPI::adventure).collect(Collectors.toList());
        im.lore(lores_);
        itemStack.setItemMeta(im);
    }

    public static void customModelData(ItemStack itemStack, int customModelData) {
        ItemMeta im = itemStack.getItemMeta();
        im.setCustomModelData(customModelData);
        itemStack.setItemMeta(im);
    }

    public static boolean isBan(ItemStack itemStack) {
        if (itemStack == null) return false;
        var list = BanConfig.getListByType(BanType.ITEM);
        if (list.isEmpty()) return false;
        return list.contains(itemStack.getType().getKey().asString());
    }

    public static Material getEggMaterial(net.minecraft.world.entity.EntityType<?> entitytype) {
        try {
            if (entitytype == net.minecraft.world.entity.EntityTypes.PLAYER) {
                return Material.PLAYER_HEAD;
            }
            var getMaterial = SpawnEggItem.byId(entitytype);
            if (getMaterial.isPresent()) {
                return getMaterial.get().value().getDefaultInstance().getBukkitStack().getType();
            } else {
                var key = net.minecraft.world.entity.EntityType.getKey(entitytype);
                if (BuiltInRegistries.ITEM.get(key).isEmpty()) {
                    return Material.SPAWNER;
                }
                Material material = get(key);
                return material.isAir() ? Material.SPAWNER : material;
            }
        } catch (Exception e) {
            return Material.SPAWNER;
        }
    }

    public static Enchantment getEnchantmentByName(String name) {
        try {
            return Enchantment.getByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    public static Enchantment getEnchantmentByKey(String key) {
        try {
            return Enchantment.getByKey(NamespacedKey.fromString(key));
        } catch (Exception e) {
            return getEnchantmentByName(key);
        }
    }

    public static Material get(Identifier key) {
        var item = BuiltInRegistries.ITEM.get(key);
        return item.map(i -> i.value().getDefaultInstance().asBukkitCopy().getType()).orElse(Material.AIR);
    }

    /**
     * Add attribute modifiers to items
     *
     * @param itemStack items
     * @param attribute type
     * @param value     attribute value
     * @param slot      Equipment Slot
     */
    public static void attribute(ItemStack itemStack, Attribute attribute, double value, EquipmentSlot slot) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        UUID uuid = UUID.randomUUID();
        String name = attribute.name().toLowerCase().replace("_", " ");
        AttributeModifier modifier = new AttributeModifier(uuid, "youer." + name, value, AttributeModifier.Operation.ADD_NUMBER, slot);
        meta.addAttributeModifier(attribute, modifier);
        itemStack.setItemMeta(meta);
    }

    /**
     * Remove the specified attribute modifier from the item
     *
     * @param itemStack items
     * @param attribute type
     * @return Returns true if successfully removed, otherwise false
     */
    public static boolean removeAttribute(ItemStack itemStack, Attribute attribute) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (!meta.hasAttributeModifiers() || meta.getAttributeModifiers(attribute) == null) {
            return false;
        }

        Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
        if (modifiers == null || modifiers.isEmpty()) {
            return false;
        }

        meta.removeAttributeModifier(attribute);
        itemStack.setItemMeta(meta);
        return true;
    }

    /**
     * Set the head texture, support Base64, URL, or texture hash
     *
     * @param meta        meta for head items
     * @param textureData (can be Base64 encoded, URL, or texture hash)
     */
    public static void setSkullTexture(SkullMeta meta, String textureData) {
        if (textureData == null || textureData.isEmpty()) {
            return;
        }
        try {
            String textureValue = Base64Utils.decodeBase64(textureData);
            if (textureValue != null) {
                Json json = Json.read(textureValue).at("textures").at("SKIN");
                var playerProfile = Bukkit.createProfile(UUID.randomUUID());
                playerProfile.getTextures().setSkin(URI.create(json.asString("url")).toURL());
                meta.setPlayerProfile(playerProfile);
            }
        } catch (Exception ignored) {
        }
    }
}
