package com.mohistmc.youer.eventhandler.dispatcher;

import com.google.common.collect.Lists;
import com.mohistmc.youer.bukkit.block.MohistBlockSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onBlockDrops(BlockDropsEvent event) {
        org.bukkit.block.Block block = CraftBlock.at(event.getLevel(), event.getPos());
        org.bukkit.block.BlockState state;
        try {
            state = block.getState();
        } catch (IllegalStateException e) {
            state = CraftBlockStates.getBlockState(event.getLevel(), event.getPos());
        }
        Entity entity = event.getBreaker();
        if (entity != null) {
            org.bukkit.entity.Player player = entity instanceof ServerPlayer serverPlayer ? serverPlayer.getBukkitEntity() : null;
            if (player != null) {
                BlockDropItemEvent bukkitEvent = new BlockDropItemEvent(block, state, player, Lists.transform(event.getDrops(), (item) -> (org.bukkit.entity.Item) item.getBukkitEntity()));
                Bukkit.getPluginManager().callEvent(bukkitEvent);
                bukkitEvent.setCancelled(event.isCanceled());
                event.setCanceled(bukkitEvent.isCancelled());
                /* TODO
                if (event.getPapersource() != null) {
                    org.bukkit.block.Block sourceblock = CraftBlock.at(event.getLevel(), event.getPapersource());
                    io.papermc.paper.event.block.BlockBreakBlockEvent eventPaper = new io.papermc.paper.event.block.BlockBreakBlockEvent(block, sourceblock, Lists.transform(event.getDrops(), (item) -> CraftItemStack.asBukkitCopy(item.getItem())));
                    eventPaper.setExpToDrop(event.getDroppedExperience()); // Paper - Properly handle xp dropping
                    eventPaper.callEvent();
                    event.setDroppedExperience(eventPaper.getExpToDrop());
                }
                 */
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer && !(entity instanceof FakePlayer) && event.getLevel() instanceof ServerLevel serverLevel) {
            Direction direction = event.getPlaceEventDirection();
            if (direction != null) {
                InteractionHand hand = event.getPlaceEventHand();
                CraftBlock placedBlock = MohistBlockSnapshot.fromBlockSnapshot(event.getBlockSnapshot(), true);
                BlockState replacedBlockState = placedBlock.getState();
                BlockPlaceEvent placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(serverLevel, serverPlayer, hand, replacedBlockState, event.getPos());
                Bukkit.getPluginManager().callEvent(placeEvent);
                if (event.isCanceled()) {
                    placeEvent.setCancelled(true);
                }
                if (!event.isCanceled() && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                    // PAIL: Remove this when MC-99075 fixed
                    serverPlayer.containerMenu.forceHeldSlot(hand);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer && !(entity instanceof FakePlayer) && event.getLevel() instanceof ServerLevel serverLevel) {
            Direction direction = event.getPlaceEventDirection();
            if (direction != null) {
                InteractionHand hand = event.getPlaceEventHand();
                List<org.bukkit.block.BlockState> placedBlocks = new ArrayList<>(event.getReplacedBlockSnapshots().size());
                for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
                    placedBlocks.add(MohistBlockSnapshot.fromBlockSnapshot(snapshot, true).getState());
                }
                BlockPlaceEvent placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockMultiPlaceEvent(serverLevel, serverPlayer, hand, placedBlocks,  event.getPos());
                Bukkit.getPluginManager().callEvent(placeEvent);
                if (event.isCanceled()) {
                    placeEvent.setCancelled(true);
                }
                if (!event.isCanceled() && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                    placeEvent.getPlayer().updateInventory();
                    event.setCanceled(true);
                }
            }
        }
    }
}
