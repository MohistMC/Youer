package org.bukkit.craftbukkit.v1_20_R4.block.data.type;

import org.bukkit.block.data.type.Bell;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftBell extends CraftBlockData implements Bell {

    private static final net.minecraft.world.level.block.state.properties.EnumProperty<?> ATTACHMENT = getEnum("attachment");

    @Override
    public org.bukkit.block.data.type.Bell.Attachment getAttachment() {
        return this.get(CraftBell.ATTACHMENT, org.bukkit.block.data.type.Bell.Attachment.class);
    }

    @Override
    public void setAttachment(org.bukkit.block.data.type.Bell.Attachment leaves) {
        this.set(CraftBell.ATTACHMENT, leaves);
    }
}