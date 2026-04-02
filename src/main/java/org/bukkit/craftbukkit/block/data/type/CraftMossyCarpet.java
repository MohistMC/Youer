package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.MossyCarpet;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftMossyCarpet extends CraftBlockData implements MossyCarpet {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty BOTTOM = getBoolean("bottom");
    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.MossyCarpet.Height>[] HEIGHTS = new org.bukkit.craftbukkit.block.data.CraftBlockStateEnum[]{
        getEnum("north", org.bukkit.block.data.type.MossyCarpet.Height.class),
        getEnum("east", org.bukkit.block.data.type.MossyCarpet.Height.class),
        getEnum("south", org.bukkit.block.data.type.MossyCarpet.Height.class),
        getEnum("west", org.bukkit.block.data.type.MossyCarpet.Height.class)
    };

    @Override
    public boolean isBottom() {
        return get(BOTTOM);
    }

    @Override
    public void setBottom(boolean up) {
        set(BOTTOM, up);
    }

    @Override
    public org.bukkit.block.data.type.MossyCarpet.Height getHeight(org.bukkit.block.BlockFace face) {
        return get(HEIGHTS[face.ordinal()]);
    }

    @Override
    public void setHeight(org.bukkit.block.BlockFace face, org.bukkit.block.data.type.MossyCarpet.Height height) {
        set(HEIGHTS[face.ordinal()], height);
    }
}
