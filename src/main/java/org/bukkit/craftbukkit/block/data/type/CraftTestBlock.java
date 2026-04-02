package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.TestBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftTestBlock extends CraftBlockData implements TestBlock {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.TestBlock.Mode> MODE = getEnum("mode", org.bukkit.block.data.type.TestBlock.Mode.class);

    @Override
    public org.bukkit.block.data.type.TestBlock.Mode getMode() {
        return get(MODE);
    }

    @Override
    public void setMode(org.bukkit.block.data.type.TestBlock.Mode mode) {
        set(MODE, mode);
    }
}
