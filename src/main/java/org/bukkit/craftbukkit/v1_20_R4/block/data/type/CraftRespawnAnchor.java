package org.bukkit.craftbukkit.v1_20_R4.block.data.type;

import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.craftbukkit.v1_20_R4.block.data.CraftBlockData;

public abstract class CraftRespawnAnchor extends CraftBlockData implements RespawnAnchor {

    private static final net.minecraft.world.level.block.state.properties.IntegerProperty CHARGES = getInteger("charges");

    @Override
    public int getCharges() {
        return this.get(CraftRespawnAnchor.CHARGES);
    }

    @Override
    public void setCharges(int charges) {
        this.set(CraftRespawnAnchor.CHARGES, charges);
    }

    @Override
    public int getMaximumCharges() {
        return getMax(CraftRespawnAnchor.CHARGES);
    }
}
