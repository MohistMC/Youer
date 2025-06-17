package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.projectile.Projectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftProjectile;

public class YouerModsProjectileEntity extends CraftProjectile {

    public YouerModsProjectileEntity(CraftServer server, Projectile entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsProjectileEntity{" + getType() + '}';
    }
}

