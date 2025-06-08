package cn.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftProjectile;

public class YouerModsThrowableEntity extends CraftProjectile {
    public YouerModsThrowableEntity(CraftServer server, ThrowableProjectile entity) {
        super(server, entity);
    }

    @Override
    public ThrowableProjectile getHandle() {
        return (ThrowableProjectile) entity;
    }

    @Override
    public String toString() {
        return "YouerModsThrowableEntity{" + getType() + '}';
    }
}
