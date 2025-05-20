package cn.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftMinecart;

public class MohistModsMinecraft extends CraftMinecart {

    public MohistModsMinecraft(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "MohistModsMinecraft{" + getType() + '}';
    }
}
