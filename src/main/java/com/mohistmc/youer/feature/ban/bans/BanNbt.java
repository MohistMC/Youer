package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.feature.ban.BanConfig;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul
 * @date 2025/12/21 22:13
 */
public class BanNbt {

    public static boolean check(ItemStack itemStack) {
        var key = itemStack.getType().key().asString();
        var yml = BanConfig.NBT.getNbtList(key);
        if (yml.isEmpty()) {
            return false;
        }
        var nbt = ItemAPI.getNbtAsString(itemStack);
        return yml.stream().anyMatch(nbt::contains);
    }

    public static boolean check(net.minecraft.world.item.ItemStack itemStack) {
        return check(itemStack.getBukkitStack());
    }
}
