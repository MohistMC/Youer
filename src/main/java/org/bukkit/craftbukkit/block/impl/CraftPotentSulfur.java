/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

public final class CraftPotentSulfur extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.type.PotentSulfur {

    public CraftPotentSulfur() {
        super();
    }

    public CraftPotentSulfur(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftPotentSulfur

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.PotentSulfur.State> POTENT_SULFUR_STATE = getEnum(net.minecraft.world.level.block.PotentSulfurBlock.class, "potent_sulfur_state", org.bukkit.block.data.type.PotentSulfur.State.class);

    @Override
    public org.bukkit.block.data.type.PotentSulfur.State getPotentSulfurState() {
        return get(POTENT_SULFUR_STATE);
    }

    @Override
    public void setPotentSulfurState(org.bukkit.block.data.type.PotentSulfur.State state) {
        set(POTENT_SULFUR_STATE, state);
    }
}
