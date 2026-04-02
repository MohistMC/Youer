package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Comparator;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftComparator extends CraftBlockData implements Comparator {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Comparator.Mode> MODE = getEnum("mode", org.bukkit.block.data.type.Comparator.Mode.class);

    @Override
    public org.bukkit.block.data.type.Comparator.Mode getMode() {
        return get(MODE);
    }

    @Override
    public void setMode(org.bukkit.block.data.type.Comparator.Mode mode) {
        set(MODE, mode);
    }
}
