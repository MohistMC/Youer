package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecartContainer;

public class YouerModsMinecartContainer extends CraftMinecartContainer {

    public YouerModsMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsMinecartContainer{" + getType() + '}';
    }
}
