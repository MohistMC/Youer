package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Endermite;

public class CraftEndermite extends CraftMonster implements Endermite {

    public CraftEndermite(CraftServer server, net.minecraft.world.entity.monster.Endermite entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.monster.Endermite getHandle() {
        return (net.minecraft.world.entity.monster.Endermite) this.entity;
    }

    @Override
    public boolean isPlayerSpawned() {
        return getHandle().isPlayerSpawned(); // Purpur - Add back player spawned endermite API
    }

    @Override
    public void setPlayerSpawned(boolean playerSpawned) {
        getHandle().setPlayerSpawned(playerSpawned); // Purpur - Add back player spawned endermite API
    }

    @Override
    public void setLifetimeTicks(int ticks) {
        this.getHandle().life = ticks;
    }

    @Override
    public int getLifetimeTicks() {
        return this.getHandle().life;
    }
}
