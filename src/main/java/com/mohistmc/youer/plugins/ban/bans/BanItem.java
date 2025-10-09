package com.mohistmc.youer.plugins.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.plugins.ban.BanConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

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
            String message = BanConfig.BAN_MESSAGE.getMessage(CraftItemStack.asCraftMirror(itemStack).getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public static boolean check(net.minecraft.world.entity.player.Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getMainHandItem();
        if (player.getBukkitEntity().isOp()) return false;
        if (checkMoShou(main)) {
            if (player.getBukkitEntity().hasPermission(moshou_permission + main.asBukkitCopy().getType().name())) {
                return false;
            }
            String message = BanConfig.BAN_MESSAGE.getMessage(CraftItemStack.asCraftMirror(main).getType().name());
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
            String message = BanConfig.BAN_MESSAGE.getMessage(CraftItemStack.asCraftMirror(off).getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            return true;
        }
        if (check(main)) {
            String message = BanConfig.BAN_MESSAGE.getMessage(CraftItemStack.asCraftMirror(main).getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            return true;
        }
        if (check(off)) {
            String message = BanConfig.BAN_MESSAGE.getMessage(CraftItemStack.asCraftMirror(off).getType().name());
            if (!message.isEmpty()) {
                player.getBukkitEntity().sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public static boolean check(ItemStack itemStack) {
        return check(CraftItemStack.asCraftMirror(itemStack));
    }

    public static boolean check(org.bukkit.inventory.ItemStack itemStack) {
        if (!YouerConfig.ban_item_enable) return false;
        return ItemAPI.isBan(itemStack);
    }

    public static boolean checkMoShou(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        if (!YouerConfig.ban_item_enable) return false;
        return BanConfig.MOSHOU.getMoShouList().contains(itemStack.getType().key().asString());
    }

    public static boolean checkMoShou(ItemStack itemStack) {
        return checkMoShou(CraftItemStack.asCraftMirror(itemStack));
    }

    public static boolean checkMoShou(net.minecraft.world.entity.player.Player player, ItemStack itemStack) {
        if (itemStack == null) return false;
        CraftHumanEntity bukkitPlayer = player.getBukkitEntity();
        if (bukkitPlayer.isOp()) return false;
        String permission = moshou_permission + CraftItemStack.asCraftMirror(itemStack).getType().name().toLowerCase();
        return checkMoShou(itemStack) && !bukkitPlayer.hasPermission(permission);
    }
}
