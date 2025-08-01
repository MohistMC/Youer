package com.mohistmc.youer.plugins.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.EnchantmentAPI;
import com.mohistmc.youer.plugins.ban.BanConfig;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 15:18:21
 */
public class BanEnchantment {


    public static boolean check(Enchantment enchantment) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        return BanConfig.ENCHANTMENT.getEnchantment().contains(enchantment.getKey().asString());
    }

    public static boolean check(ItemStack itemStack) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(itemStack)) {
                return BanConfig.ENCHANTMENT.getEnchantment().contains(enchantment.getKey().asString());
            }
        }
        return false;
    }

    public static boolean check(net.minecraft.world.item.ItemStack itemStack) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(CraftItemStack.asBukkitCopy(itemStack))) {
                return BanConfig.ENCHANTMENT.getEnchantment().contains(enchantment.getKey().asString());
            }
        }
        return false;
    }
}
