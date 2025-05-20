package org.bukkit.craftbukkit.v1_21_R1.entity;

import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;

public class CraftBlockAttachedEntity extends CraftEntity {

    public CraftBlockAttachedEntity(CraftServer server, BlockAttachedEntity entity) {
        super(server, entity);
    }

    @Override
    public BlockAttachedEntity getHandle() {
        return (BlockAttachedEntity) entity;
    }

    @Override
    public String toString() {
        return "CraftBlockAttachedEntity";
    }
}
