package org.bukkit.craftbukkit.v1_21_R1.entity;

import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.entity.Breeze;

public class CraftBreeze extends CraftMonster implements Breeze {

    public CraftBreeze(CraftServer server, net.minecraft.world.entity.monster.breeze.Breeze entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.breeze.Breeze getHandle() {
        return (net.minecraft.world.entity.monster.breeze.Breeze) this.entity;
    }

    @Override
    public String toString() {
        return "CraftBreeze";
    }
}
