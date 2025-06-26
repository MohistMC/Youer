package ca.spottedleaf.moonrise.patches.collisions;

public final class ExplosionBlockCache {

    public final long key;
    public final net.minecraft.core.BlockPos immutablePos;
    public final net.minecraft.world.level.block.state.BlockState blockState;
    public final net.minecraft.world.level.material.FluidState fluidState;
    public final float resistance;
    public final boolean outOfWorld;
    public Boolean shouldExplode; // null -> not called yet
    public net.minecraft.world.phys.shapes.VoxelShape cachedCollisionShape;

    public ExplosionBlockCache(final long key, final net.minecraft.core.BlockPos immutablePos, final net.minecraft.world.level.block.state.BlockState blockState,
                               final net.minecraft.world.level.material.FluidState fluidState, final float resistance, final boolean outOfWorld) {
        this.key = key;
        this.immutablePos = immutablePos;
        this.blockState = blockState;
        this.fluidState = fluidState;
        this.resistance = resistance;
        this.outOfWorld = outOfWorld;
    }
}
