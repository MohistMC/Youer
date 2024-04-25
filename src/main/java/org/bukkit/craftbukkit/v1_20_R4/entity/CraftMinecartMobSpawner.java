package org.bukkit.craftbukkit.v1_20_R4.entity;

import net.minecraft.world.entity.vehicle.MinecartSpawner;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.entity.minecart.SpawnerMinecart;

final class CraftMinecartMobSpawner extends CraftMinecart implements SpawnerMinecart {
    CraftMinecartMobSpawner(CraftServer server, MinecartSpawner entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "CraftMinecartMobSpawner";
    }
}
