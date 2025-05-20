package cn.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftAbstractWindCharge;

public class YouerModsWindCharge extends CraftAbstractWindCharge {

    public YouerModsWindCharge(CraftServer server, AbstractWindCharge entity) {
        super(server, entity);
    }
}
