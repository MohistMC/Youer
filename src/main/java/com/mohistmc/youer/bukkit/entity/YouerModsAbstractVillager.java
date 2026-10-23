package com.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractVillager;

public class YouerModsAbstractVillager extends CraftAbstractVillager {

    public YouerModsAbstractVillager(CraftServer server, AbstractVillager entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsAbstractVillager{" + getType() + '}';
    }
}
