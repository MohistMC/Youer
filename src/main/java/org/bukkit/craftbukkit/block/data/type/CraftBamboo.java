package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Bamboo;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftBamboo extends CraftBlockData implements Bamboo {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Bamboo.Leaves> LEAVES = getEnum("leaves", org.bukkit.block.data.type.Bamboo.Leaves.class);

    @Override
    public org.bukkit.block.data.type.Bamboo.Leaves getLeaves() {
        return get(LEAVES);
    }

    @Override
    public void setLeaves(org.bukkit.block.data.type.Bamboo.Leaves leaves) {
        set(LEAVES, leaves);
    }
}
