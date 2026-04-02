package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Switch;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftSwitch extends CraftBlockData implements Switch {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Switch.Face> FACE = getEnum("face", org.bukkit.block.data.type.Switch.Face.class);

    @Override
    public org.bukkit.block.data.type.Switch.Face getFace() {
        return get(FACE);
    }

    @Override
    public void setFace(org.bukkit.block.data.type.Switch.Face face) {
        set(FACE, face);
    }
}
