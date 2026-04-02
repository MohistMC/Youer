package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.Shelf;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftShelf extends CraftBlockData implements Shelf {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.Shelf.SideChain> SIDE_CHAIN = getEnum("side_chain", org.bukkit.block.data.type.Shelf.SideChain.class);

    @Override
    public org.bukkit.block.data.type.Shelf.SideChain getSideChain() {
        return get(SIDE_CHAIN);
    }

    @Override
    public void setSideChain(org.bukkit.block.data.type.Shelf.SideChain sideChain) {
        set(SIDE_CHAIN, sideChain);
    }
}
