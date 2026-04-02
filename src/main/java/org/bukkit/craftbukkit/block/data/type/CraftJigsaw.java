package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Jigsaw;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftJigsaw extends CraftBlockData implements Jigsaw {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Jigsaw.Orientation> ORIENTATION = getEnum("orientation", org.bukkit.block.data.type.Jigsaw.Orientation.class);

    @Override
    public org.bukkit.block.data.type.Jigsaw.Orientation getOrientation() {
        return get(ORIENTATION);
    }

    @Override
    public void setOrientation(org.bukkit.block.data.type.Jigsaw.Orientation orientation) {
        set(ORIENTATION, orientation);
    }
}
