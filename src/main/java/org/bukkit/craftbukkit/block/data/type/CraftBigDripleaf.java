package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.BigDripleaf;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftBigDripleaf extends CraftBlockData implements BigDripleaf {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.BigDripleaf.Tilt> TILT = getEnum("tilt", org.bukkit.block.data.type.BigDripleaf.Tilt.class);

    @Override
    public Tilt getTilt() {
        return get(TILT);
    }

    @Override
    public void setTilt(org.bukkit.block.data.type.BigDripleaf.Tilt tilt) {
        set(TILT, tilt);
    }
}
