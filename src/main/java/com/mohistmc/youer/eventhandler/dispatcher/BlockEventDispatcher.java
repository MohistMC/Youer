package com.mohistmc.youer.eventhandler.dispatcher;

import com.mohistmc.youer.bukkit.block.MohistBlockSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
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
        if (event.getLevel().captureDrops != null) {
            return; // Paper - player block breaks are handled by ServerPlayerGameMode with a full drop capture
        }
        org.bukkit.block.Block block = CraftBlock.at(event.getLevel(), event.getPos());
        org.bukkit.block.BlockState state;
        try {
            state = block.getState();
        } catch (IllegalStateException e) {
            state = CraftBlockStates.getBlockState(event.getLevel(), event.getPos());
        }
        Entity entity = event.getBreaker();
        if (entity instanceof ServerPlayer serverPlayer) {
            List<org.bukkit.entity.Item> items = new ArrayList<>(event.getDrops().size());
            for (ItemEntity drop : event.getDrops()) {
                items.add((org.bukkit.entity.Item) drop.getBukkitEntity());
            }
            BlockDropItemEvent bukkitEvent = new BlockDropItemEvent(block, state, serverPlayer.getBukkitEntity(), items);
            bukkitEvent.setCancelled(event.isCanceled());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            event.setCanceled(bukkitEvent.isCancelled());
            if (!event.isCanceled()) {
                // Keep NeoForge's drop list in sync with the list plugins saw
                List<ItemEntity> remaining = new ArrayList<>(items.size());
                for (org.bukkit.entity.Item item : items) {
                    if (!item.isValid()) {
                        remaining.add(((org.bukkit.craftbukkit.entity.CraftItem) item).getHandle());
                    }
                }
                event.getDrops().clear();
                event.getDrops().addAll(remaining);
            }
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

    @SubscribeEvent(receiveCanceled = true)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer && event.getLevel() instanceof ServerLevel serverLevel) {
            Direction direction = event.getPlaceEventDirection();
            if (direction != null && !(entity instanceof FakePlayer)) {
                InteractionHand hand = event.getPlaceEventHand();
                CraftBlock placedBlock = MohistBlockSnapshot.fromBlockSnapshot(event.getBlockSnapshot(), true);
                BlockState replacedBlockState = placedBlock.getState();
                BlockPlaceEvent placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(serverLevel, serverPlayer, hand, replacedBlockState, event.getPos());
                if (event.isCanceled()) {
                    placeEvent.setCancelled(true);
                }
                if (!event.isCanceled() && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                    // PAIL: Remove this when MC-99075 fixed
                    serverPlayer.containerMenu.forceHeldSlot(hand);
                    event.setCanceled(true);
                }
            }
            if (!event.isCanceled()) {
                BlockPos pos = event.getPos();
                // Paper start - run these after the place event so a cancelled placement neither spawns the wither nor opens the editor
                if (serverLevel.getBlockEntity(pos) instanceof SkullBlockEntity placedSkull) {
                    WitherSkullBlock.checkSpawn(serverLevel, pos, placedSkull);
                }
                if (!(entity instanceof FakePlayer)
                    && serverLevel.getBlockState(pos).getBlock() instanceof SignBlock sign
                    && serverLevel.getBlockEntity(pos) instanceof SignBlockEntity signEntity
                    && !signEntity.isWaxed()
                    && signEntity.getText(SignTextSlot.FRONT).hasEditableText(serverPlayer.isTextFilteringEnabled())) {
                    sign.openTextEdit(serverPlayer, signEntity, SignTextSlot.FRONT, io.papermc.paper.event.player.PlayerOpenSignEvent.Cause.PLACE);
                }
                // Paper end
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
                BlockPlaceEvent placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockMultiPlaceEvent(serverLevel, serverPlayer, hand, placedBlocks, event.getPos());
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
