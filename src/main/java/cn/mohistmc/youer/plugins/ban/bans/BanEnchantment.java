package cn.mohistmc.youer.plugins.ban.bans;

import cn.mohistmc.youer.YouerConfig;
import cn.mohistmc.youer.api.EnchantmentAPI;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 15:18:21
 */
public class BanEnchantment {

    public static boolean check(net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return check(CraftEnchantment.minecraftToBukkit(enchantment));
    }

    public static boolean check(Enchantment enchantment) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        return YouerConfig.ban_enchantment_list.contains(enchantment.getName());
    }

    public static boolean check(ItemStack itemStack) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(itemStack)) {
                return YouerConfig.ban_enchantment_list.contains(enchantment.getName());
            }
        }
        return false;
    }

    public static boolean check(net.minecraft.world.item.ItemStack itemStack) {
        if (!YouerConfig.ban_enchantment_enable) return false;
        if (EnchantmentAPI.has(itemStack)) {
            for (Enchantment enchantment : EnchantmentAPI.get(CraftItemStack.asBukkitCopy(itemStack))) {
                return YouerConfig.ban_enchantment_list.contains(enchantment.getName());
            }
        }
        return false;
    }
}
