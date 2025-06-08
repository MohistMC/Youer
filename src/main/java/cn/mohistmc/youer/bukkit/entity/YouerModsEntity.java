package cn.mohistmc.youer.bukkit.entity;


import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity;

public class YouerModsEntity extends CraftEntity {

    public YouerModsEntity(CraftServer server, net.minecraft.world.entity.Entity entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "YouerModsEntity{" + this.getType() + '}';
    }
}
