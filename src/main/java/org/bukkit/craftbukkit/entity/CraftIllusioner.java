package org.bukkit.craftbukkit.entity;

import com.destroystokyo.paper.entity.CraftRangedEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Illusioner;

public class CraftIllusioner extends CraftSpellcaster implements Illusioner, CraftRangedEntity<net.minecraft.world.entity.monster.Illusioner> {

    public CraftIllusioner(CraftServer server, net.minecraft.world.entity.monster.Illusioner entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Illusioner getHandle() {
        return (net.minecraft.world.entity.monster.Illusioner) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftIllusioner";
    }
}
