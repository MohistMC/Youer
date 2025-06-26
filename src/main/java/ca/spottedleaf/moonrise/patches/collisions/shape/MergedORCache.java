package ca.spottedleaf.moonrise.patches.collisions.shape;

public record MergedORCache(
    net.minecraft.world.phys.shapes.VoxelShape key,
    net.minecraft.world.phys.shapes.VoxelShape result
) {

}
