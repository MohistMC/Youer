package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecart;


public class YouerModsMinecart extends CraftMinecart {

    public YouerModsMinecart(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsMinecart{" + getType() + '}';
    }
}
