package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 禁止食用指定物品
 *
 * @author Mgazul
 */
public class BanEat {

    /**
     * @param itemKey 物品 id，如 {@code minecraft:apple}；支持 {@code minecraft:*} 通配整个命名空间
     */
    public static boolean check(String itemKey) {
        if (itemKey == null || itemKey.isEmpty()) return false;
        if (!YouerConfig.ban_eat_enable) return false;
        var list = BanConfig.getListByType(BanType.EAT);
        if (list.isEmpty()) return false;
        int i = itemKey.indexOf(':');
        return list.contains(itemKey) || (i > 0 && list.contains(itemKey.substring(0, i) + ":*"));
    }

    public static boolean check(net.minecraft.world.item.ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return check(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString());
    }

    public static boolean check(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return check(itemStack.getType().key().asString());
    }

    // op 与 FakePlayer 直接放行
    public static boolean check(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack itemStack) {
        if (player == null) return false;
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer) return false;
        if (player.getBukkitEntity().isOp()) return false;
        return check(itemStack);
    }

    public static boolean check(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack itemStack) {
        if (player == null) return false;
        if (player.isOp()) return false;
        return check(itemStack);
    }
}
