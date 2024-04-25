package org.bukkit.craftbukkit.v1_20_R4.entity;

import net.minecraft.world.entity.animal.frog.Tadpole;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;

public class CraftTadpole extends CraftFish implements org.bukkit.entity.Tadpole {

    public CraftTadpole(CraftServer server, Tadpole entity) {
        super(server, entity);
    }

    @Override
    public Tadpole getHandle() {
        return (Tadpole) this.entity;
    }

    @Override
    public String toString() {
        return "CraftTadpole";
    }

    @Override
    public int getAge() {
        return this.getHandle().age;
    }

    @Override
    public void setAge(int age) {
        this.getHandle().age = age;
    }
}