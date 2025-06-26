package ca.spottedleaf.moonrise.patches.collisions.shape;

public record CachedToAABBs(
        java.util.List<net.minecraft.world.phys.AABB> aabbs,
        boolean isOffset,
        double offX, double offY, double offZ
) {

    public ca.spottedleaf.moonrise.patches.collisions.shape.CachedToAABBs removeOffset() {
        final java.util.List<net.minecraft.world.phys.AABB> toOffset = this.aabbs;
        final double offX = this.offX;
        final double offY = this.offY;
        final double offZ = this.offZ;

        final java.util.List<net.minecraft.world.phys.AABB> ret = new java.util.ArrayList<>(toOffset.size());

        for (int i = 0, len = toOffset.size(); i < len; ++i) {
            ret.add(toOffset.get(i).move(offX, offY, offZ));
        }

        return new ca.spottedleaf.moonrise.patches.collisions.shape.CachedToAABBs(ret, false, 0.0, 0.0, 0.0);
    }

    public static ca.spottedleaf.moonrise.patches.collisions.shape.CachedToAABBs offset(final ca.spottedleaf.moonrise.patches.collisions.shape.CachedToAABBs cache, final double offX, final double offY, final double offZ) {
        if (offX == 0.0 && offY == 0.0 && offZ == 0.0) {
            return cache;
        }

        final double resX = cache.offX + offX;
        final double resY = cache.offY + offY;
        final double resZ = cache.offZ + offZ;

        return new ca.spottedleaf.moonrise.patches.collisions.shape.CachedToAABBs(cache.aabbs, true, resX, resY, resZ);
    }
}
