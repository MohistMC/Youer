/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class CraftCoralPlant extends CraftBlockData implements org.bukkit.block.data.Waterlogged {

    public CraftCoralPlant() {
        super();
    }

    public CraftCoralPlant(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftWaterlogged

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED = getBoolean(net.minecraft.world.level.block.CoralPlantBlock.class, "waterlogged");

    @Override
    public boolean isWaterlogged() {
        return this.get(CraftCoralPlant.WATERLOGGED);
    }

    @Override
    public void setWaterlogged(boolean waterlogged) {
        this.set(CraftCoralPlant.WATERLOGGED, waterlogged);
    }
}
