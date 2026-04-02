package org.bukkit.craftbukkit.block.data;

import org.bukkit.block.data.Rail;

public abstract class CraftRail extends CraftBlockData implements Rail {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.Rail.Shape> SHAPE = getEnum("shape", org.bukkit.block.data.Rail.Shape.class);

    @Override
    public org.bukkit.block.data.Rail.Shape getShape() {
        return get(SHAPE);
    }

    @Override
    public void setShape(org.bukkit.block.data.Rail.Shape shape) {
        set(SHAPE, shape);
    }

    @Override
    public java.util.Set<org.bukkit.block.data.Rail.Shape> getShapes() {
        return getValues(SHAPE);
    }
}
