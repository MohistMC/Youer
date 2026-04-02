package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Bed;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftBed extends CraftBlockData implements Bed {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Bed.Part> PART = getEnum("part", org.bukkit.block.data.type.Bed.Part.class);
    private static final net.minecraft.world.level.block.state.properties.BooleanProperty OCCUPIED = getBoolean("occupied");

    @Override
    public org.bukkit.block.data.type.Bed.Part getPart() {
        return get(PART);
    }

    @Override
    public void setPart(org.bukkit.block.data.type.Bed.Part part) {
        set(PART, part);
    }

    @Override
    public boolean isOccupied() {
        return get(OCCUPIED);
    }
}
