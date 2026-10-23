package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractCubeMob;

public class YouerModsAbstractCubeMob extends CraftAbstractCubeMob {

    public YouerModsAbstractCubeMob(CraftServer server, AbstractCubeMob entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsAbstractCubeMob{" + getType() + '}';
    }
}
