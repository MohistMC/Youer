package ca.spottedleaf.moonrise.patches.collisions.shape;

public interface CollisionVoxelShape {

    public double moonrise$offsetX();

    public double moonrise$offsetY();

    public double moonrise$offsetZ();

    public double[] moonrise$rootCoordinatesX();

    public double[] moonrise$rootCoordinatesY();

    public double[] moonrise$rootCoordinatesZ();

    public ca.spottedleaf.moonrise.patches.collisions.shape.CachedShapeData moonrise$getCachedVoxelData();

    // rets null if not possible to represent this shape as one AABB
    public net.minecraft.world.phys.AABB moonrise$getSingleAABBRepresentation();

    // ONLY USE INTERNALLY, ONLY FOR INITIALISING IN CONSTRUCTOR: VOXELSHAPES ARE STATIC
    public void moonrise$initCache();

    // this returns empty if not clamped to 1.0 or 0.0 depending on direction
    public net.minecraft.world.phys.shapes.VoxelShape moonrise$getFaceShapeClamped(final net.minecraft.core.Direction direction);

    public boolean moonrise$isFullBlock();

    public boolean moonrise$occludesFullBlock();

    public boolean moonrise$occludesFullBlockIfCached();

    // uses a cache internally
    public net.minecraft.world.phys.shapes.VoxelShape moonrise$orUnoptimized(final net.minecraft.world.phys.shapes.VoxelShape other);
}
