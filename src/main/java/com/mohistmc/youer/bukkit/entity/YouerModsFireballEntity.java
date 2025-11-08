package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.projectile.Fireball;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftFireball;

/**
 * @author Mgazul
 * @date 2025/11/7 18:00
 */
public class YouerModsFireballEntity extends CraftFireball {

    public YouerModsFireballEntity(CraftServer server, Fireball entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsFireballEntity{" + getType() + '}';
    }
}
