/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

public final class CraftMossyCarpet extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.type.MossyCarpet {

    public CraftMossyCarpet() {
        super();
    }

    public CraftMossyCarpet(net.minecraft.world.level.block.state.IBlockData state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftMossyCarpet

    private static final net.minecraft.world.level.block.state.properties.BlockStateBoolean BOTTOM = getBoolean(net.minecraft.world.level.block.MossyCarpetBlock.class, "bottom");
    private static final net.minecraft.world.level.block.state.properties.BlockStateEnum<?>[] HEIGHTS = new net.minecraft.world.level.block.state.properties.BlockStateEnum[]{
        getEnum(net.minecraft.world.level.block.MossyCarpetBlock.class, "north"), getEnum(net.minecraft.world.level.block.MossyCarpetBlock.class, "east"), getEnum(net.minecraft.world.level.block.MossyCarpetBlock.class, "south"), getEnum(net.minecraft.world.level.block.MossyCarpetBlock.class, "west")
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
        return get(HEIGHTS[face.ordinal()], org.bukkit.block.data.type.MossyCarpet.Height.class);
    }

    @Override
    public void setHeight(org.bukkit.block.BlockFace face, org.bukkit.block.data.type.MossyCarpet.Height height) {
        set(HEIGHTS[face.ordinal()], height);
    }
}