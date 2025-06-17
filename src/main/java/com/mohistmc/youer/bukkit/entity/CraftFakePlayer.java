package com.mohistmc.youer.bukkit.entity;

import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class CraftFakePlayer extends CraftPlayer {

    public CraftFakePlayer(CraftServer server, FakePlayer entity) {
        super(server, entity);
    }

    @Override
    public boolean isOp() {
        GameProfile profile = this.getHandle().getGameProfile();
        return profile != null && profile.getId() != null && super.isOp();
    }

    @Override
    public void setOp(boolean value) {
    }

    public String toString() {
        return "CraftFakePlayer{" + "name=" + getName() + '}';
    }
}
