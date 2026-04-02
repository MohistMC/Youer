package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftStairs extends CraftBlockData implements Stairs {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Stairs.Shape> SHAPE = getEnum("shape", org.bukkit.block.data.type.Stairs.Shape.class);

    @Override
    public org.bukkit.block.data.type.Stairs.Shape getShape() {
        return get(SHAPE);
    }

    @Override
    public void setShape(org.bukkit.block.data.type.Stairs.Shape shape) {
        set(SHAPE, shape);
    }
}
