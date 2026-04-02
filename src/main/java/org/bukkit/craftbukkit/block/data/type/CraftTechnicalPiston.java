package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.TechnicalPiston;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftTechnicalPiston extends CraftBlockData implements TechnicalPiston {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.TechnicalPiston.Type> TYPE = getEnum("type", org.bukkit.block.data.type.TechnicalPiston.Type.class);

    @Override
    public org.bukkit.block.data.type.TechnicalPiston.Type getType() {
        return get(TYPE);
    }

    @Override
    public void setType(org.bukkit.block.data.type.TechnicalPiston.Type type) {
        set(TYPE, type);
    }
}
