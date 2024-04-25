package org.bukkit.craftbukkit.v1_20_R4.entity;

import net.minecraft.world.entity.ambient.AmbientCreature;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.entity.Ambient;

public class CraftAmbient extends CraftMob implements Ambient {
    public CraftAmbient(CraftServer server, AmbientCreature entity) {
        super(server, entity);
    }

    @Override
    public AmbientCreature getHandle() {
        return (AmbientCreature) this.entity;
    }

    @Override
    public String toString() {
        return "CraftAmbient";
    }
}
