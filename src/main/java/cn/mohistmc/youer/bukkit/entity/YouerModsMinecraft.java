package cn.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftMinecart;

public class YouerModsMinecraft extends CraftMinecart {

    public YouerModsMinecraft(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsMinecraft{" + getType() + '}';
    }
}
