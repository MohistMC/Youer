/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class CraftSweetBerryBush extends CraftBlockData implements org.bukkit.block.data.Ageable {

    public CraftSweetBerryBush() {
        super();
    }

    public CraftSweetBerryBush(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftAgeable

    private static final net.minecraft.world.level.block.state.properties.IntegerProperty AGE = getInteger(net.minecraft.world.level.block.SweetBerryBushBlock.class, "age");

    @Override
    public int getAge() {
        return this.get(CraftSweetBerryBush.AGE);
    }

    @Override
    public void setAge(int age) {
        this.set(CraftSweetBerryBush.AGE, age);
    }

    @Override
    public int getMaximumAge() {
        return getMax(CraftSweetBerryBush.AGE);
    }
}
