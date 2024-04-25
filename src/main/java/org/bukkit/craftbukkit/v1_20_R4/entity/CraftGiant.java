package org.bukkit.craftbukkit.v1_20_R4.entity;

import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.entity.Giant;

public class CraftGiant extends CraftMonster implements Giant {

    public CraftGiant(CraftServer server, net.minecraft.world.entity.monster.Giant entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Giant getHandle() {
        return (net.minecraft.world.entity.monster.Giant) this.entity;
    }

    @Override
    public String toString() {
        return "CraftGiant";
    }
}
