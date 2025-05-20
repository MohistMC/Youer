/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_21_R1.block.impl;

import org.bukkit.craftbukkit.v1_21_R1.block.data.CraftBlockData;

public final class CraftLayeredCauldron extends CraftBlockData implements org.bukkit.block.data.Levelled {

    public CraftLayeredCauldron() {
        super();
    }

    public CraftLayeredCauldron(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftLevelled

    private static final net.minecraft.world.level.block.state.properties.IntegerProperty LEVEL = getInteger(net.minecraft.world.level.block.LayeredCauldronBlock.class, "level");

    @Override
    public int getLevel() {
        return this.get(CraftLayeredCauldron.LEVEL);
    }

    @Override
    public void setLevel(int level) {
        this.set(CraftLayeredCauldron.LEVEL, level);
    }

    @Override
    public int getMaximumLevel() {
        return getMax(CraftLayeredCauldron.LEVEL);
    }
}
