package org.bukkit.craftbukkit.v1_20_R4.block.data.type;

import org.bukkit.block.data.type.CommandBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftCommandBlock extends CraftBlockData implements CommandBlock {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty CONDITIONAL = getBoolean("conditional");

    @Override
    public boolean isConditional() {
        return this.get(CraftCommandBlock.CONDITIONAL);
    }

    @Override
    public void setConditional(boolean conditional) {
        this.set(CraftCommandBlock.CONDITIONAL, conditional);
    }
}
