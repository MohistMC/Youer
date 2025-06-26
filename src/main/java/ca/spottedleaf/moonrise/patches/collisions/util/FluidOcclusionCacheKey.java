package ca.spottedleaf.moonrise.patches.collisions.util;

public record FluidOcclusionCacheKey(net.minecraft.world.level.block.state.BlockState first, net.minecraft.world.level.block.state.BlockState second, net.minecraft.core.Direction direction, boolean result) {
}
