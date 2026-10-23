package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.animal.cow.AbstractCow;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractCow;

public class YouerModsAbstractCow extends CraftAbstractCow {

    public YouerModsAbstractCow(CraftServer server, AbstractCow entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsAbstractCow{" + getType() + '}';
    }
}
