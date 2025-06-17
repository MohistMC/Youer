package com.mohistmc.youer.bukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractSkeleton;
import org.bukkit.entity.Skeleton;
import org.jetbrains.annotations.NotNull;

public class YouerModsSkeleton extends CraftAbstractSkeleton {
    public YouerModsSkeleton(CraftServer server, net.minecraft.world.entity.monster.AbstractSkeleton entity) {
        super(server, entity);
    }

    public @NotNull Skeleton.SkeletonType getSkeletonType() {
        return Skeleton.SkeletonType.FORGE_MODS;
    }

    @Override
    public String toString() {
        return "YouerModsSkeleton{" + getType() + '}';
    }
}
