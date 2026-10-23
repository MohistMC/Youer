package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftChestBoat;

public class YouerModsChestBoat extends CraftChestBoat {

    public YouerModsChestBoat(CraftServer server, AbstractChestBoat entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsChestBoat{" + getType() + '}';
    }
}
