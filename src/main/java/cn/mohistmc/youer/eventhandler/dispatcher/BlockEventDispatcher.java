package cn.mohistmc.youer.eventhandler.dispatcher;

import cn.mohistmc.youer.bukkit.block.MohistBlockSnapshot;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_21_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_21_R1.block.CraftBlockStates;
import org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityInteractEvent;

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
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getPlayer();
        // CraftBukkit start - fire BlockBreakEvent
        org.bukkit.block.Block bblock = CraftBlock.at(level, pos);
        if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
            if (level instanceof ServerLevel) {
                BlockBreakEvent bukkitEvent = new BlockBreakEvent(bblock, serverPlayer.getBukkitEntity());
                bukkitEvent.setCancelled(event.isCanceled());
                event.setDropItems(bukkitEvent.isDropItems());
                Bukkit.getPluginManager().callEvent(bukkitEvent);
                event.setCanceled(bukkitEvent.isCancelled());
            }
            // CraftBukkit end
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onBlockPlace(BlockDropsEvent event) {
        org.bukkit.block.Block block = org.bukkit.craftbukkit.v1_21_R1.block.CraftBlock.at(event.getLevel(), event.getPos());
        org.bukkit.block.BlockState state = block.getState();
        Entity entity = event.getBreaker();
        if (entity != null) {
            org.bukkit.entity.Player player = entity instanceof ServerPlayer serverPlayer ? serverPlayer.getBukkitEntity() : null;
            if (player != null) {
                BlockDropItemEvent bukkitEvent = new BlockDropItemEvent(block, state, player, Lists.transform(event.getDrops(), (item) -> (org.bukkit.entity.Item) item.getBukkitEntity()));
                Bukkit.getPluginManager().callEvent(bukkitEvent);
                bukkitEvent.setCancelled(event.isCanceled());
                event.setCanceled(bukkitEvent.isCancelled());
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
                replacedBlockState.setData(event.getBlockSnapshot().getState());
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
                if (hand == InteractionHand.MAIN_HAND) {
                    bukkitStack = player.getInventory().getItemInMainHand();
                } else {
                    bukkitStack = player.getInventory().getItemInOffHand();
                }
                BlockPlaceEvent placeEvent = new BlockMultiPlaceEvent(placedBlocks, againstBlock, bukkitStack, player, !event.isCanceled()
                );
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

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockPos blockposition = event.getPos();
        BlockHitResult hitResult = event.getHitVec();
        InteractionHand interactionhand = event.getHand();
        ItemStack itemstack = player.getItemInHand(interactionhand);
        org.bukkit.event.player.PlayerInteractEvent eventBukkit = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, blockposition, hitResult.getDirection(), itemstack, event.getUseItem().isFalse(), interactionhand, hitResult.getLocation());
        event.setCanceled(eventBukkit.isCancelled());
    }

    @SubscribeEvent
    public void onProjectileHit(ExplosionEvent.Start event) {

    }
}
