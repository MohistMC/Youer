/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class CraftJukeBox extends CraftBlockData implements org.bukkit.block.data.type.Jukebox {

    public CraftJukeBox() {
        super();
    }

    public CraftJukeBox(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftJukebox

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty HAS_RECORD = getBoolean(net.minecraft.world.level.block.JukeboxBlock.class, "has_record");

    @Override
    public boolean hasRecord() {
        return this.get(CraftJukeBox.HAS_RECORD);
    }
}
