package org.bukkit.craftbukkit.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.World;

public final class CraftLocation {

    private CraftLocation() {
    }

    public static Location toBukkit(Vec3D vec3D) {
        return toBukkit(vec3D, null);
    }

    public static Location toBukkit(Vec3D vec3D, World world) {
        return toBukkit(vec3D, world, 0.0F, 0.0F);
    }

    public static Location toBukkit(Vec3D vec3D, World world, float yaw, float pitch) {
        return new Location(world, vec3D.x(), vec3D.y(), vec3D.z(), yaw, pitch);
    }

    public static Location toBukkit(BlockPos blockPosition) {
        return toBukkit(blockPosition, (World) null);
    }
    public static Location toBukkit(BlockPos blockPosition, net.minecraft.world.level.World world) {
        return toBukkit(blockPosition, world.getWorld(), 0.0F, 0.0F);
    }
    public static Location toBukkit(BlockPos blockPosition, World world) {
        return toBukkit(blockPosition, world, 0.0F, 0.0F);
    }

    public static Location toBukkit(BlockPos blockPosition, World world, float yaw, float pitch) {
        return new Location(world, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), yaw, pitch);
    }

    public static BlockPos toBlockPos(Location location) {
        return new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Vec3D toVec3D(Location location) {
        return new Vec3D(location.getX(), location.getY(), location.getZ());
    }
}
