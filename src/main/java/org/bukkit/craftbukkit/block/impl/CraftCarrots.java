/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class CraftCarrots extends CraftBlockData implements org.bukkit.block.data.Ageable {

    public CraftCarrots() {
        super();
    }

    public CraftCarrots(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftAgeable

    private static final net.minecraft.world.level.block.state.properties.IntegerProperty AGE = getInteger(net.minecraft.world.level.block.CarrotBlock.class, "age");

    @Override
    public int getAge() {
        return this.get(CraftCarrots.AGE);
    }

    @Override
    public void setAge(int age) {
        this.set(CraftCarrots.AGE, age);
    }

    @Override
    public int getMaximumAge() {
        return getMax(CraftCarrots.AGE);
    }
}
