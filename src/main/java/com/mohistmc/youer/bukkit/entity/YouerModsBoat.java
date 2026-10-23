package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftBoat;

public class YouerModsBoat extends CraftBoat {

    public YouerModsBoat(CraftServer server, AbstractBoat entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsBoat{" + getType() + '}';
    }
}
