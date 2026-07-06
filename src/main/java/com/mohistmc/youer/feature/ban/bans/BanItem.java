package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 2:54:23
 */
public class BanItem {

    private static final String moshou_permission = "youer.ban.item.moshou.";

    public static boolean check(UseOnContext use) {
        return check(use.getPlayer(), use.getItemInHand());
    }

    public static boolean check(net.minecraft.world.entity.player.Player player, ItemStack itemStack) {
        if (player == null) return false;
        if (player.getBukkitEntity().isOp()) return false;
        if (BanEnchantment.check(itemStack)) {
            player.containerMenu.sendAllDataToRemote();
            return true;
        }
        if (check(itemStack)) {
            player.containerMenu.sendAllDataToRemote();
            String message = BanConfig.BAN_MESSAGE.getMessage(itemStack.getBukkitStack().getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public static boolean check(net.minecraft.world.entity.player.Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (player.getBukkitEntity().isOp()) return false;
        if (checkMoShou(main)) {
            if (player.getBukkitEntity().hasPermission(moshou_permission + main.asBukkitCopy().getType().name())) {
                return false;
            }
            String message = BanConfig.BAN_MESSAGE.getMessage(main.getBukkitStack().getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            return true;
        }
        if (checkMoShou(off)) {
            if (player.getBukkitEntity().hasPermission(moshou_permission + off.asBukkitCopy().getType().name())) {
                return false;
            }
            String message = BanConfig.BAN_MESSAGE.getMessage(off.getBukkitStack().getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            return true;
        }
        if (check(main)) {
            String message = BanConfig.BAN_MESSAGE.getMessage(main.getBukkitStack().getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            return true;
        }
        if (check(off)) {
            String message = BanConfig.BAN_MESSAGE.getMessage(off.getBukkitStack().getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public static boolean check(ItemStack itemStack) {
        if (!YouerConfig.ban_item_enable) return false;
        return ItemAPI.isBan(itemStack.getBukkitStack()) || BanNbt.check(itemStack);
    }

    public static boolean checkMoShou(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        if (!YouerConfig.ban_item_enable) return false;
        var list = BanConfig.getListByType(BanType.ITEM_MOSHOU);
        if (list.isEmpty()) return false;
        return list.contains(itemStack.getType().key().asString());
    }

    public static boolean checkMoShou(ItemStack itemStack) {
        return checkMoShou(itemStack.getBukkitStack());
    }

    public static boolean checkMoShou(net.minecraft.world.entity.player.Player player, ItemStack itemStack) {
        if (itemStack == null) return false;
        CraftHumanEntity bukkitPlayer = player.getBukkitEntity();
        if (bukkitPlayer.isOp()) return false;
        String permission = moshou_permission + itemStack.getBukkitStack().getType().name().toLowerCase();
        return checkMoShou(itemStack) && !bukkitPlayer.hasPermission(permission);
    }
}
