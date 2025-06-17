/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2024.
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

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;

public class ItemEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onItemExpireEvent(ItemExpireEvent event) {
        if (CraftEventFactory.callItemDespawnEvent(event.getEntity()).isCancelled()) {
            event.setExtraLife(-1);
        }
    }
}
