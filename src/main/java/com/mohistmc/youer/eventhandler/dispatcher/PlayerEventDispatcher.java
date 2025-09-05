/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2025.
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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;

public class PlayerEventDispatcher {

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        // Youer start - Custom Container compatible with mods
        AbstractContainerMenu abstractcontainermenu = event.getContainer();
        abstractcontainermenu.containerOwner = event.getEntity();
        if (abstractcontainermenu.getBukkitView() == null) {
            org.bukkit.inventory.Inventory inventory = new CraftInventory(new YouerModsInventory(abstractcontainermenu, event.getEntity()));
            inventory.getType().setMods(true);
            abstractcontainermenu.bukkitView = new CraftInventoryView<>(event.getEntity().getBukkitEntity(), inventory, abstractcontainermenu);
        }
        // Youer end
        CraftEventFactory.handleInventoryCloseEvent(event.getEntity(), event.getClose$Reason()); // CraftBukkit
    }
}
