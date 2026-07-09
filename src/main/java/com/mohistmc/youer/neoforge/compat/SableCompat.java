package com.mohistmc.youer.neoforge.compat;

import com.mohistmc.youer.api.WorldAPI;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.util.CraftLocation;

/**
 * @author Mgazul
 * @date 2026/4/27 23:57
 */
public class SableCompat {

    public static Vec3 at(Level level, Vec3 pos) {
        return Sable.HELPER.projectOutOfSubLevel(level, pos);
    }

    public static Location at(Location pos) {
        if (pos == null) return null;
        World world = pos.getWorld();
        if (world == null) return pos;
        Vec3 vec3 = Sable.HELPER.projectOutOfSubLevel(WorldAPI.getServerLevel(world), CraftLocation.toVec3D(pos));
        return CraftLocation.toBukkit(vec3, world, pos.getYaw(), pos.getPitch());
    }
}
