package org.bukkit.craftbukkit.v1_20_R4.block.data.type;

import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftCaveVinesPlant extends CraftBlockData implements CaveVinesPlant {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty BERRIES = getBoolean("berries");

    @Override
    public boolean isBerries() {
        return this.get(CraftCaveVinesPlant.BERRIES);
    }

    @Override
    public void setBerries(boolean berries) {
        this.set(CraftCaveVinesPlant.BERRIES, berries);
    }
}