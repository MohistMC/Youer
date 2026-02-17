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

import com.mohistmc.youer.bukkit.inventory.YouerModsInventory;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;

public class PlayerEventDispatcher {

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        var player = event.getEntity();
        var bukkit = player.getBukkitEntity();
        // Youer start - Custom Container compatible with mods
        AbstractContainerMenu abstractcontainermenu = event.getContainer();
        abstractcontainermenu.containerOwner = player;
        if (abstractcontainermenu.getBukkitView() == null) {
            org.bukkit.inventory.Inventory inventory = new CraftInventory(new YouerModsInventory(abstractcontainermenu, player));
            inventory.getType().setMods(true);
            abstractcontainermenu.bukkitView = new CraftInventoryView<>(bukkit, inventory, abstractcontainermenu);
        }
        // Youer end
        if (player instanceof FakePlayer || bukkit == null) return;
        CraftEventFactory.handleInventoryCloseEvent(player, event.getClose$Reason()); // CraftBukkit
    }

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
}
