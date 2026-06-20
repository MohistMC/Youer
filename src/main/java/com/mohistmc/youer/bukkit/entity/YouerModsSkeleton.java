package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractSkeleton;
import org.bukkit.entity.Skeleton;
import org.jetbrains.annotations.NotNull;

public class YouerModsSkeleton extends CraftAbstractSkeleton {
    public YouerModsSkeleton(CraftServer server, AbstractSkeleton entity) {
        super(server, entity);
    }

    public @NotNull Skeleton.SkeletonType getSkeletonType() {
        return Skeleton.SkeletonType.NORMAL;
    }

    @Override
    public String toString() {
        return "YouerModsSkeleton{" + getType() + '}';
    }
}
