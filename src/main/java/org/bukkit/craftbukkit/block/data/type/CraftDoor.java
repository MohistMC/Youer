package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Door;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftDoor extends CraftBlockData implements Door {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Door.Hinge> HINGE = getEnum("hinge", org.bukkit.block.data.type.Door.Hinge.class);

    @Override
    public org.bukkit.block.data.type.Door.Hinge getHinge() {
        return get(HINGE);
    }

    @Override
    public void setHinge(org.bukkit.block.data.type.Door.Hinge hinge) {
        set(HINGE, hinge);
    }
}
