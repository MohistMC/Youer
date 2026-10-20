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
        if (!event.isFireCBEvent()) {
            return;
        }
        // StartAttacking fires the Bukkit event on its own, firing it here again would double it
        if (event.getTargetType() == LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        EntityTargetEvent.TargetReason reason = mob.getTargetReason();
        if (reason == null) {
            // no Bukkit event has to be fired for this target change
            return;
        }
        event.setReason(reason);
        LivingEntity entityliving = event.getNewAboutToBeSetTarget();
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
                // CraftBukkit start - a plugin may set a target that is not valid for this mob
                event.setNewAboutToBeSetTarget(mob.pruneInvalidTarget(((CraftLivingEntity) CBevent.getTarget()).getHandle()));
                // CraftBukkit end
            } else {
                event.setNewAboutToBeSetTarget(null);
            }
        }
    }
}
