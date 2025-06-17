package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractWindCharge;

public class YouerModsWindCharge extends CraftAbstractWindCharge {

    public YouerModsWindCharge(CraftServer server, AbstractWindCharge entity) {
        super(server, entity);
    }
}
