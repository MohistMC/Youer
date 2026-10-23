package com.mohistmc.youer.bukkit.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;

public class YouerModsBlockEntityState extends CraftBlockEntityState<BlockEntity> {

    public YouerModsBlockEntityState(World world, BlockEntity blockEntity) {
        super(world, blockEntity);
    }

    protected YouerModsBlockEntityState(YouerModsBlockEntityState state, Location location) {
        super(state, location);
    }

    @Override
    public YouerModsBlockEntityState copy() {
        return new YouerModsBlockEntityState(this, null);
    }

    @Override
    public YouerModsBlockEntityState copy(Location location) {
        return new YouerModsBlockEntityState(this, location);
    }
}
