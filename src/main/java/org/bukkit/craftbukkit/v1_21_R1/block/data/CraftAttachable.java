package org.bukkit.craftbukkit.v1_21_R1.block.data;

import org.bukkit.block.data.Attachable;

public abstract class CraftAttachable extends CraftBlockData implements Attachable {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty ATTACHED = getBoolean("attached");

    @Override
    public boolean isAttached() {
        return this.get(CraftAttachable.ATTACHED);
    }

    @Override
    public void setAttached(boolean attached) {
        this.set(CraftAttachable.ATTACHED, attached);
    }
}
