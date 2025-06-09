/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class CraftBarrier extends CraftBlockData implements org.bukkit.block.data.Waterlogged {

    public CraftBarrier() {
        super();
    }

    public CraftBarrier(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftWaterlogged

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED = getBoolean(net.minecraft.world.level.block.BarrierBlock.class, "waterlogged");

    @Override
    public boolean isWaterlogged() {
        return this.get(CraftBarrier.WATERLOGGED);
    }

    @Override
    public void setWaterlogged(boolean waterlogged) {
        this.set(CraftBarrier.WATERLOGGED, waterlogged);
    }
}
