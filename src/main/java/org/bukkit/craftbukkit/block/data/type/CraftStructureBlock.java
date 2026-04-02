package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.StructureBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftStructureBlock extends CraftBlockData implements StructureBlock {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.StructureBlock.Mode> MODE = getEnum("mode", org.bukkit.block.data.type.StructureBlock.Mode.class);

    @Override
    public org.bukkit.block.data.type.StructureBlock.Mode getMode() {
        return get(MODE);
    }

    @Override
    public void setMode(org.bukkit.block.data.type.StructureBlock.Mode mode) {
        set(MODE, mode);
    }
}
