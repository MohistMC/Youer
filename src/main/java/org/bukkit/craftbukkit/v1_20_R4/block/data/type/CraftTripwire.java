package org.bukkit.craftbukkit.v1_20_R4.block.data.type;

import org.bukkit.block.data.type.Tripwire;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftTripwire extends CraftBlockData implements Tripwire {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty DISARMED = getBoolean("disarmed");

    @Override
    public boolean isDisarmed() {
        return this.get(CraftTripwire.DISARMED);
    }

    @Override
    public void setDisarmed(boolean disarmed) {
        this.set(CraftTripwire.DISARMED, disarmed);
    }
}