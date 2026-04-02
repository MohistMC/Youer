package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Slab;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftSlab extends CraftBlockData implements Slab {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Slab.Type> TYPE = getEnum("type", org.bukkit.block.data.type.Slab.Type.class);

    @Override
    public org.bukkit.block.data.type.Slab.Type getType() {
        return get(TYPE);
    }

    @Override
    public void setType(org.bukkit.block.data.type.Slab.Type type) {
        set(TYPE, type);
    }
}
