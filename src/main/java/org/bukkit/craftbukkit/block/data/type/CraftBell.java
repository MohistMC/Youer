package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Bell;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftBell extends CraftBlockData implements Bell {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Bell.Attachment> ATTACHMENT = getEnum("attachment", org.bukkit.block.data.type.Bell.Attachment.class);

    @Override
    public org.bukkit.block.data.type.Bell.Attachment getAttachment() {
        return get(ATTACHMENT);
    }

    @Override
    public void setAttachment(org.bukkit.block.data.type.Bell.Attachment leaves) {
        set(ATTACHMENT, leaves);
    }
}
