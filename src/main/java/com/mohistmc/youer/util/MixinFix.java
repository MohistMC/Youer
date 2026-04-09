package com.mohistmc.youer.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * @author Mgazul
 * @date 2025/9/24 17:53
 */
public class MixinFix {

    public static boolean fixPlace(BlockItem blockItem, ServerLevel level, ServerPlayer player, BlockPlaceContext blockplacecontext, BlockPos blockpos, BlockState place$blockstateCB) {
        BlockPlaceEvent placeEvent = CraftEventFactory.callBlockPlaceEvent(level, player, blockplacecontext.getHand(), place$blockstateCB, blockpos.getX(), blockpos.getY(), blockpos.getZ());
        if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
            place$blockstateCB.update(true, false);

            if (blockItem instanceof SolidBucketItem) {
                player.getBukkitEntity().updateInventory(); // SPIGOT-4541
            }
            return true;
        }
        return false;
    }
}
