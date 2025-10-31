package com.mohistmc.youer.api;

import com.mohistmc.mjson.Json;
import com.mohistmc.tools.Base64Utils;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.plugins.ban.BanConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class ItemAPI {

    public static final Logger LOGGER = LogManager.getLogger("ItemAPI");

    public static net.minecraft.world.item.ItemStack toNMSItem(Material material) {
        ItemStack itemStack = new ItemStack(material);
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public static net.minecraft.world.item.ItemStack toNMSItem(ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public static ItemStack getBukkit(Material material) {
        return new ItemStack(material);
    }

    public static String getNbtAsString(CompoundTag compoundTag) {
        return compoundTag == null ? "null" : compoundTag.getAsString();
    }

    /**
     * Parse Base64 into {@link org.bukkit.inventory.ItemStack}
     * it should be noted that this method is only used for ItemStack without any NBT
     *
     * @param base64
     * @return
     * @throws IOException
     */
    public static ItemStack getBukkitByBase64(String base64) {
        try {
            try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64)))) {
                return (ItemStack) dataInput.readObject();
            }
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("Unable to decode class type.");
            return getBukkit(Material.AIR);
        }
    }

    /**
     * Parse {@link org.bukkit.inventory.ItemStack} into Base64
     * it should be noted that this method is only used for ItemStack without any NBT
     *
     * @param stack
     * @return
     */
    public static String getBase64byBukkit(ItemStack stack) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(stack);

            // Serialize that array
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static String serializeNbt(CompoundTag nbtTagCompound) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbtTagCompound, buf);
            return Base64.getEncoder().encodeToString(buf.toByteArray());
        } catch (IOException ignored) {
            return null;
        }
    }

    public static CompoundTag deserializeNbt(String serializeNBT) {
        if (serializeNBT != null) {
            ByteArrayInputStream buf = new ByteArrayInputStream(Base64.getDecoder().decode(serializeNBT));
            try {
                return NbtIo.readCompressed(buf, NbtAccounter.unlimitedHeap());
            } catch (IOException e) {
                Youer.LOGGER.error("Reading nbt ", e);
            }
        }
        return null;
    }

    public static byte[] nbtToByte(CompoundTag nbt) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            nbt.write(dos);
            byte[] outputByteArray = baos.toByteArray();
            dos.close();
            baos.close();
            return outputByteArray;
        } catch (IOException e) {
            Youer.LOGGER.error("nbtToByte ", e);
            return null;
        }
    }

    public static CompoundTag byteToNbt(byte[] nbtByte) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(nbtByte, 0, nbtByte.length);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            CompoundTag reconstructedCompoundTag = NbtIo.read(dataInputStream);
            dataInputStream.close();
            byteArrayInputStream.close();
            return reconstructedCompoundTag;
        } catch (IOException e) {
            Youer.LOGGER.error("byteToNbt ", e);
            return null;
        }
    }

    public static void name(ItemStack itemStack, String name) {
        ItemMeta im = itemStack.getItemMeta();
        im.displayName(ColorAPI.colorize(name));
        itemStack.setItemMeta(im);
    }

    public static void rarity(ItemStack itemStack, ItemRarity rarity) {
        ItemMeta im = itemStack.getItemMeta();
        im.setRarity(rarity);
        itemStack.setItemMeta(im);
    }

    public static void lore(ItemStack itemStack, List<String> lores) {
        ItemMeta im = itemStack.getItemMeta();
        List<Component> lores_ = lores.stream().map(ColorAPI::colorize).collect(Collectors.toList());
        im.lore(lores_);
        itemStack.setItemMeta(im);
    }

    public static void customModelData(ItemStack itemStack, int customModelData) {
        ItemMeta im = itemStack.getItemMeta();
        im.setCustomModelData(customModelData);
        itemStack.setItemMeta(im);
    }

    @Deprecated
    public static TextComponent show(ItemStack itemStack) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
        CompoundTag compound = new CompoundTag();
        nmsItemStack.save(MinecraftServer.getServer().registryAccess(), compound);
        String json = compound.toString();
        BaseComponent[] hoverEventComponents = new BaseComponent[]{
                new TextComponent(json)
        };
        TextComponent component = new TextComponent(itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() ? itemStack.getItemMeta().getDisplayName() : itemStack.getTranslationKey());
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents));
        return component;
    }

    public static boolean isBan(ItemStack itemStack) {
        if (itemStack == null) return false;
        return BanConfig.ITEM.getItem().contains(itemStack.getType().getKey().asString());
    }

    public static Material getEggMaterial(net.minecraft.world.entity.EntityType<?> entitytype) {
        try {
            if (entitytype == net.minecraft.world.entity.EntityType.PLAYER) {
                return Material.PLAYER_HEAD;
            }
            var getMaterial = SpawnEggItem.byId(entitytype);
            if (getMaterial != null) {
                return getMaterial.getDefaultInstance().getBukkitStack().getType();
            } else {
                var key = net.minecraft.world.entity.EntityType.getKey(entitytype);
                if (BuiltInRegistries.ITEM.get(key) == null) {
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

    public static Material get(ResourceLocation key) {
        return BuiltInRegistries.ITEM.get(key).getDefaultInstance().asBukkitCopy().getType();
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
                var playerProfile = Bukkit.createPlayerProfile(UUID.randomUUID());
                playerProfile.getTextures().setSkin(URI.create(json.asString("url")).toURL());
                meta.setOwnerProfile(playerProfile);
            }
        } catch (Exception ignored) {
        }
    }
}