package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractArrow;

public class YouerModsAbstractArrow extends CraftAbstractArrow {

    public YouerModsAbstractArrow(CraftServer server, AbstractArrow entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsAbstractArrow{" + getType() + '}';
    }
}
