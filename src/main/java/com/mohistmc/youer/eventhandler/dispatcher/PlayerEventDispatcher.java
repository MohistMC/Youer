/*
 * Copyright (C) MohistMC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.youer.eventhandler.dispatcher;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerEventDispatcher {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEnterSleepEvent(CanPlayerSleepEvent event) {
        var serverPlayer = event.getEntity();
        var blockposition = event.getPos();
        Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, Unit> nmsBedResult = event.getProblem() != null ? Either.left(event.getProblem()) : Either.right(Unit.INSTANCE);
        var cbedResult = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerBedEnterEvent(serverPlayer, blockposition, nmsBedResult);
        if (cbedResult.left().isPresent()) {
            event.setProblem(cbedResult.left().get());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onItemTossEvent(ItemTossEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        ItemEntity itemEntity = event.getEntity();

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.captureDrops = null;
        }

        org.bukkit.entity.Player bPlayer = player.getBukkitEntity();
        org.bukkit.entity.Item bItem = (org.bukkit.entity.Item) itemEntity.getBukkitEntity();

        PlayerDropItemEvent bukkitEvent = new PlayerDropItemEvent(bPlayer, bItem);
        Bukkit.getPluginManager().callEvent(bukkitEvent);

        if (bukkitEvent.isCancelled()) {
            event.setCanceled(true);
            ItemStack stack = itemEntity.getItem();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false, false, false, null);
            }
            player.containerMenu.broadcastChanges();
        }
    }
}
