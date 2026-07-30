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

import java.util.Collection;
import java.util.Iterator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;

public class ItemEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onItemExpireEvent(ItemExpireEvent event) {
        if (CraftEventFactory.callItemDespawnEvent(event.getEntity()).isCancelled()) {
            event.setExtraLife(-1);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onLivingDropsEvent(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.captureDrops = null;
        }

        Collection<ItemEntity> drops = event.getDrops();
        Iterator<ItemEntity> iterator = drops.iterator();

        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            org.bukkit.event.entity.EntityDropItemEvent bukkitEvent =
                new org.bukkit.event.entity.EntityDropItemEvent(
                    entity.getBukkitEntity(),
                    (org.bukkit.entity.Item) itemEntity.getBukkitEntity()
                );
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            if (bukkitEvent.isCancelled()) {
                iterator.remove();
            }
        }
    }
}
