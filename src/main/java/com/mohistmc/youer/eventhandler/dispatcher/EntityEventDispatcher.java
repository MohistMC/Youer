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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class EntityEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void changeTargetEvent(LivingChangeTargetEvent event) {
        EntityTargetEvent.TargetReason reason = event.getReason();
        LivingEntity entityliving = event.getNewAboutToBeSetTarget();

        if (entityliving instanceof Mob mob) {
            if (event.isFireCBEvent()) {
                if (reason == EntityTargetEvent.TargetReason.UNKNOWN && mob.getTarget() != null && entityliving == null) {
                    reason = mob.getTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;
                }
                CraftLivingEntity ctarget = null;
                if (entityliving != null) {
                    ctarget = (CraftLivingEntity) entityliving.getBukkitEntity();
                }
                EntityTargetLivingEntityEvent CBevent = new EntityTargetLivingEntityEvent(event.getEntity().getBukkitEntity(), ctarget, reason);
                Bukkit.getPluginManager().callEvent(CBevent);
                if (CBevent.isCancelled()) {
                    event.setCanceled(true);
                } else {
                    if (CBevent.getTarget() != null) {
                        event.setNewAboutToBeSetTarget(((CraftLivingEntity) CBevent.getTarget()).getHandle());
                    }
                }
            }
        }
    }
}
