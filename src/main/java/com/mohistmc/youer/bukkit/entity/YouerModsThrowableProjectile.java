package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftThrowableProjectile;

public class YouerModsThrowableProjectile extends CraftThrowableProjectile {

    public YouerModsThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsThrowableProjectile{" + getType() + '}';
    }
}
