package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractHorse;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.Horse;
import org.jetbrains.annotations.NotNull;

public class YouerModsAbstractHorse extends CraftAbstractHorse {

    public YouerModsAbstractHorse(CraftServer server, AbstractHorse entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsAbstractHorse{" + getType() + '}';
    }

    @Override
    public @NotNull Horse.Variant getVariant() {
        return Horse.Variant.MODS;
    }

    @Override
    public @NotNull EntityCategory getCategory() {
        return EntityCategory.NONE;
    }
}
