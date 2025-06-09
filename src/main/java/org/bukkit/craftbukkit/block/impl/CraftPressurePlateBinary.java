/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class CraftPressurePlateBinary extends CraftBlockData implements org.bukkit.block.data.Powerable {

    public CraftPressurePlateBinary() {
        super();
    }

    public CraftPressurePlateBinary(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftPowerable

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = getBoolean(net.minecraft.world.level.block.PressurePlateBlock.class, "powered");

    @Override
    public boolean isPowered() {
        return this.get(CraftPressurePlateBinary.POWERED);
    }

    @Override
    public void setPowered(boolean powered) {
        this.set(CraftPressurePlateBinary.POWERED, powered);
    }
}
