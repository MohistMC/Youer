/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class CraftPressurePlateWeighted extends CraftBlockData implements org.bukkit.block.data.AnaloguePowerable {

    public CraftPressurePlateWeighted() {
        super();
    }

    public CraftPressurePlateWeighted(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftAnaloguePowerable

    private static final net.minecraft.world.level.block.state.properties.IntegerProperty POWER = getInteger(net.minecraft.world.level.block.WeightedPressurePlateBlock.class, "power");

    @Override
    public int getPower() {
        return this.get(CraftPressurePlateWeighted.POWER);
    }

    @Override
    public void setPower(int power) {
        this.set(CraftPressurePlateWeighted.POWER, power);
    }

    @Override
    public int getMaximumPower() {
        return getMax(CraftPressurePlateWeighted.POWER);
    }
}
