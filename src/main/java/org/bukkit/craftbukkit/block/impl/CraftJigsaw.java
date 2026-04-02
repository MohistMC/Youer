/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

public final class CraftJigsaw extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.type.Jigsaw {

    public CraftJigsaw() {
        super();
    }

    public CraftJigsaw(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftJigsaw

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Jigsaw.Orientation> ORIENTATION = getEnum(net.minecraft.world.level.block.JigsawBlock.class, "orientation", org.bukkit.block.data.type.Jigsaw.Orientation.class);

    @Override
    public org.bukkit.block.data.type.Jigsaw.Orientation getOrientation() {
        return get(ORIENTATION);
    }

    @Override
    public void setOrientation(org.bukkit.block.data.type.Jigsaw.Orientation orientation) {
        set(ORIENTATION, orientation);
    }
}
