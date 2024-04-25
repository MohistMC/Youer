package org.bukkit.craftbukkit.v1_20_R4.block.data.type;

import org.bukkit.block.data.type.Snow;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public class CraftSnow extends CraftBlockData implements Snow {

    private static final net.minecraft.world.level.block.state.properties.IntegerProperty LAYERS = getInteger("layers");

    @Override
    public int getLayers() {
        return this.get(CraftSnow.LAYERS);
    }

    @Override
    public void setLayers(int layers) {
        this.set(CraftSnow.LAYERS, layers);
    }

    @Override
    public int getMinimumLayers() {
        return getMin(CraftSnow.LAYERS);
    }

    @Override
    public int getMaximumLayers() {
        return getMax(CraftSnow.LAYERS);
    }
}
