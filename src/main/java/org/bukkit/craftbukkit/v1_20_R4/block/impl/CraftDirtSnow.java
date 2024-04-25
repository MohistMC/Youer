/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_20_R4.block.impl;

public final class CraftDirtSnow extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.Snowable {

    public CraftDirtSnow() {
        super();
    }

    public CraftDirtSnow(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftSnowable

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty SNOWY = getBoolean(net.minecraft.world.level.block.SnowyDirtBlock.class, "snowy");

    @Override
    public boolean isSnowy() {
        return this.get(CraftDirtSnow.SNOWY);
    }

    @Override
    public void setSnowy(boolean snowy) {
        this.set(CraftDirtSnow.SNOWY, snowy);
    }
}
