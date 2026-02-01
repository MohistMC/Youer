package com.mohistmc.youer.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 15:30:06
 */
public class EnchantmentAPI {

    public static boolean has(org.bukkit.inventory.ItemStack itemStack) {
        return (itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants()) || itemStack.getType() == org.bukkit.Material.ENCHANTED_BOOK;
    }

    public static boolean has(net.minecraft.world.item.ItemStack itemStack) {
        return itemStack.has(DataComponents.ENCHANTMENTS);
    }

    public static List<org.bukkit.enchantments.Enchantment> get(org.bukkit.inventory.ItemStack itemStack) {
        var nms = CraftItemStack.asNMSCopy(itemStack);
        List<org.bukkit.enchantments.Enchantment> enchantmentsS = new ArrayList<>();
        if (EnchantmentHelper.hasAnyEnchantments(nms)) {
            var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(nms);
            for (var enchantment : enchantments.keySet()) {
                enchantmentsS.add(CraftEnchantment.minecraftToBukkit(enchantment.value()));
            }
        }
        return enchantmentsS;
    }
}
