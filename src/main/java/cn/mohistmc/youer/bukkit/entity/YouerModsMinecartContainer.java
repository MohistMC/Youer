package cn.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftMinecartContainer;

public class YouerModsMinecartContainer extends CraftMinecartContainer {

    public YouerModsMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsMinecartContainer{" + getType() + '}';
    }
}
