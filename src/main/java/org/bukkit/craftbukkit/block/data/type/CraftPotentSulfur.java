package org.bukkit.craftbukkit.block.data.type;

import org.bukkit.block.data.type.PotentSulfur;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftPotentSulfur extends CraftBlockData implements PotentSulfur {

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.PotentSulfur.State> POTENT_SULFUR_STATE = getEnum("potent_sulfur_state", org.bukkit.block.data.type.PotentSulfur.State.class);

    @Override
    public org.bukkit.block.data.type.PotentSulfur.State getPotentSulfurState() {
        return get(POTENT_SULFUR_STATE);
    }

    @Override
    public void setPotentSulfurState(org.bukkit.block.data.type.PotentSulfur.State state) {
        set(POTENT_SULFUR_STATE, state);
    }
}
