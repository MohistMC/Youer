package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.feature.ban.BanConfig;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
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
        var nms = CraftItemStack.asNMSCopy(itemStack);
        var componentString = nms.getComponents().toString();

        return yml.stream().anyMatch(componentString::contains);
    }
}
