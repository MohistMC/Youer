/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_20_R4.block.impl;

public final class CraftCaveVinesPlant extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.type.CaveVinesPlant {

    public CraftCaveVinesPlant() {
        super();
    }

    public CraftCaveVinesPlant(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftCaveVinesPlant

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty BERRIES = getBoolean(net.minecraft.world.level.block.CaveVinesPlantBlock.class, "berries");

    @Override
    public boolean isBerries() {
        return this.get(CraftCaveVinesPlant.BERRIES);
    }

    @Override
    public void setBerries(boolean berries) {
        this.set(CraftCaveVinesPlant.BERRIES, berries);
    }
}
