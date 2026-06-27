package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.IronGolem;

public class CraftIronGolem extends CraftGolem implements IronGolem {
    public CraftIronGolem(CraftServer server, net.minecraft.world.entity.animal.golem.IronGolem entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.golem.IronGolem getHandle() {
        return (net.minecraft.world.entity.animal.golem.IronGolem) this.entity;
    }

    @Override
    public boolean isPlayerCreated() {
        return this.getHandle().isPlayerCreated();
    }

    @Override
    public void setPlayerCreated(boolean playerCreated) {
        this.getHandle().setPlayerCreated(playerCreated);
    }

    // Purpur start - Summoner API
    @Override
    @org.jetbrains.annotations.Nullable
    public java.util.UUID getSummoner() {
        return getHandle().getSummoner();
    }

    @Override
    public void setSummoner(@org.jetbrains.annotations.Nullable java.util.UUID summoner) {
        getHandle().setSummoner(summoner);
    }
    // Purpur end - Summoner API
}
