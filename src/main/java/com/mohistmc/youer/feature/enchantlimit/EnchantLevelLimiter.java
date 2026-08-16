package com.mohistmc.youer.feature.enchantlimit;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Limits the maximum level of an enchantment, per enchantment.
 *
 * <p>Applied at the same places as {@code BanEnchantment}, i.e. the {@code getLevel}
 * and {@code Mutable} methods of {@code ItemEnchantments}, so both enchantment level
 * reads and writes are clamped to the configured maximum. Enchantments without a
 * configured limit are left untouched.</p>
 */
public class EnchantLevelLimiter {

    private EnchantLevelLimiter() {
    }

    public static int clamp(Holder<Enchantment> enchantment, int level) {
        Integer max = EnchantLimitConfig.getMaxLevel(enchantment.getRegisteredName());
        if (max == null) return level;
        return Math.min(max, level);
    }
}
