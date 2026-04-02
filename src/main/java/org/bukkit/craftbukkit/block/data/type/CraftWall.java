package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Wall;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftWall extends CraftBlockData implements Wall {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty UP = getBoolean("up");
    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Wall.Height>[] HEIGHTS = new org.bukkit.craftbukkit.block.data.CraftBlockStateEnum[]{
        getEnum("north", org.bukkit.block.data.type.Wall.Height.class),
        getEnum("east", org.bukkit.block.data.type.Wall.Height.class),
        getEnum("south", org.bukkit.block.data.type.Wall.Height.class),
        getEnum("west", org.bukkit.block.data.type.Wall.Height.class)
    };

    @Override
    public boolean isUp() {
        return get(UP);
    }

    @Override
    public void setUp(boolean up) {
        set(UP, up);
    }

    @Override
    public org.bukkit.block.data.type.Wall.Height getHeight(org.bukkit.block.BlockFace face) {
        return get(HEIGHTS[face.ordinal()]);
    }

    @Override
    public void setHeight(org.bukkit.block.BlockFace face, org.bukkit.block.data.type.Wall.Height height) {
        set(HEIGHTS[face.ordinal()], height);
    }
}
