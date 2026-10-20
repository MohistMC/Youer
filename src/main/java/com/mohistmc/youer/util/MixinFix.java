package com.mohistmc.youer.util;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
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
            level.restoringBlockSnapshots = true; // Youer - undo of a cancelled place must not run onRemove
            place$blockstateCB.update(true, false);
            level.restoringBlockSnapshots = false;
            player.getBukkitEntity().updateInventory(); // SPIGOT-4541
            return true;
        }
        return false;
    }

    public static net.kyori.adventure.text.Component message(AdvancementHolder p_300979_, ServerPlayer player) {
        return p_300979_.value().display().filter(DisplayInfo::shouldAnnounceChat).map(info -> io.papermc.paper.adventure.PaperAdventure.asAdventure(info.getType().createAnnouncement(p_300979_, player))).orElse(null);
    }
}
