package com.mohistmc.youer.eventhandler.dispatcher;

import com.google.common.collect.Lists;
import com.mohistmc.youer.bukkit.block.MohistBlockSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BlockEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onProjectileHit(ProjectileImpactEvent event) {
        HitResult hitResult = event.getRayTraceResult();
        Block block = event.getProjectile().getBlockStateOn().getBlock();
        Level level = event.getProjectile().level();
        Projectile projectile = event.getProjectile();
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            if (block instanceof AbstractCandleBlock) {
                // CraftBukkit start
                if (CraftEventFactory.callBlockIgniteEvent(level, projectile.getOnPos(), projectile).isCancelled()) {
                    event.setCanceled(true);
                }
                // CraftBukkit end
            }
        }
    }

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
        if (entity instanceof ServerPlayer serverPlayer && !(entity instanceof FakePlayer)) {
            org.bukkit.entity.Player player = serverPlayer.getBukkitEntity();
            Direction direction = event.getPlaceEventDirection();
            if (direction != null) {
                InteractionHand hand = event.getPlaceEventHand();
                CraftBlock placedBlock = MohistBlockSnapshot.fromBlockSnapshot(event.getBlockSnapshot(), true);
                CraftBlock againstBlock = CraftBlock.at(event.getLevel(), event.getPos().relative(direction.getOpposite()));
                org.bukkit.inventory.ItemStack bukkitStack;
                org.bukkit.inventory.EquipmentSlot bukkitHand;
                if (hand == InteractionHand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                    bukkitHand = org.bukkit.inventory.EquipmentSlot.HAND;
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                    bukkitHand = org.bukkit.inventory.EquipmentSlot.OFF_HAND;
                }
                CraftBlockState replacedBlockState = CraftBlockStates.getBlockState(event.getLevel(), event.getPos());
               // replacedBlockState.setData(event.getBlockSnapshot().getState());
                BlockPlaceEvent placeEvent = new BlockPlaceEvent(placedBlock, replacedBlockState, againstBlock, bukkitStack, player, !event.isCanceled(), bukkitHand);
                placeEvent.setCancelled(event.isCanceled());
                Bukkit.getPluginManager().callEvent(placeEvent);
                if (placeEvent.isCancelled()) {
                    player.updateInventory();
                }
                event.setCanceled(placeEvent.isCancelled() || !placeEvent.canBuild());
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer serverPlayer && !(entity instanceof FakePlayer)) {
            org.bukkit.entity.Player player = serverPlayer.getBukkitEntity();
            Direction direction = event.getPlaceEventDirection();
            if (direction != null) {
                InteractionHand hand = event.getPlaceEventHand();
                List<org.bukkit.block.BlockState> placedBlocks = new ArrayList<>(event.getReplacedBlockSnapshots().size());
                for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
                    placedBlocks.add(MohistBlockSnapshot.fromBlockSnapshot(snapshot, true).getState());
                }
                CraftBlock againstBlock = CraftBlock.at(event.getLevel(), event.getPos().relative(direction.getOpposite()));
                org.bukkit.inventory.ItemStack bukkitStack;
                // Paper start - add hand to BlockMultiPlaceEvent
                EquipmentSlot equipmentSlot;
                if (hand == InteractionHand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                    equipmentSlot = EquipmentSlot.HAND;
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                    equipmentSlot = EquipmentSlot.OFF_HAND;
                }
                BlockPlaceEvent placeEvent = new BlockMultiPlaceEvent(placedBlocks, againstBlock, bukkitStack, player, !event.isCanceled()
                );
                // Paper end
                placeEvent.setCancelled(event.isCanceled());
                Bukkit.getPluginManager().callEvent(placeEvent);
                event.setCanceled(placeEvent.isCancelled() || !placeEvent.canBuild());
            }
        }
    }

    @SubscribeEvent
    public void onFarmlandBreak(BlockEvent.FarmlandTrampleEvent event) {
        Entity entity = event.getEntity();
        Cancellable cancellable;
        if (entity instanceof Player player) {
            cancellable = CraftEventFactory.callPlayerInteractEvent(player, org.bukkit.event.block.Action.PHYSICAL, event.getPos(), null, null, null);
        } else {
            cancellable = new EntityInteractEvent(entity.getBukkitEntity(), CraftBlock.at(event.getLevel(), event.getPos()));
            Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
        }

        if (cancellable.isCancelled()) {
            event.setCanceled(true);
            return;
        }

        if (!CraftEventFactory.callEntityChangeBlockEvent(entity, event.getPos(), Blocks.DIRT.defaultBlockState())) {
            event.setCanceled(true);
        }
    }
}
