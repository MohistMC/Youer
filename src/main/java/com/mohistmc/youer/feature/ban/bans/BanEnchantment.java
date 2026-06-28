package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.EnchantmentAPI;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import net.minecraft.core.Holder;
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
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        return list.contains(enchantment.getKey().asString());
    }

    public static boolean check(Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        return list.contains(enchantment.getRegisteredName());
    }

    public static boolean check(ItemStack itemStack) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(itemStack)) {
                return list.contains(enchantment.getKey().asString());
            }
        }
        return false;
    }

    public static boolean check(net.minecraft.world.item.ItemStack itemStack) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        var list = BanConfig.getListByType(BanType.ENCHANTMENT);
        if (list.isEmpty()) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(CraftItemStack.asBukkitCopy(itemStack))) {
                return list.contains(enchantment.getKey().asString());
            }
        }
        return false;
    }
}
