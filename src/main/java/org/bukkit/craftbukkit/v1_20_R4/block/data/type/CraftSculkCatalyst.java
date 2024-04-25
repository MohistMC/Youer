package org.bukkit.craftbukkit.v1_20_R4.block.data.type;

import org.bukkit.block.data.type.SculkCatalyst;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftSculkCatalyst extends CraftBlockData implements SculkCatalyst {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty BLOOM = getBoolean("bloom");

    @Override
    public boolean isBloom() {
        return this.get(CraftSculkCatalyst.BLOOM);
    }

    @Override
    public void setBloom(boolean bloom) {
        this.set(CraftSculkCatalyst.BLOOM, bloom);
    }
}