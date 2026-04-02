/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

public final class CraftMovingPiston extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.type.TechnicalPiston, org.bukkit.block.data.Directional {

    public CraftMovingPiston() {
        super();
    }

    public CraftMovingPiston(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftTechnicalPiston

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.TechnicalPiston.Type> TYPE = getEnum(net.minecraft.world.level.block.piston.MovingPistonBlock.class, "type", org.bukkit.block.data.type.TechnicalPiston.Type.class);

    @Override
    public org.bukkit.block.data.type.TechnicalPiston.Type getType() {
        return get(TYPE);
    }

    @Override
    public void setType(org.bukkit.block.data.type.TechnicalPiston.Type type) {
        set(TYPE, type);
    }

    // org.bukkit.craftbukkit.block.data.CraftDirectional

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.BlockFace> FACING = getEnum(net.minecraft.world.level.block.piston.MovingPistonBlock.class, "facing", org.bukkit.block.BlockFace.class);

    @Override
    public org.bukkit.block.BlockFace getFacing() {
        return get(FACING);
    }

    @Override
    public void setFacing(org.bukkit.block.BlockFace facing) {
        set(FACING, facing);
    }

    @Override
    public java.util.Set<org.bukkit.block.BlockFace> getFaces() {
        return getValues(FACING);
    }
}
