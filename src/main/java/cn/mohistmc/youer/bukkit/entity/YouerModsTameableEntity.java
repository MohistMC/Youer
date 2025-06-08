package cn.mohistmc.youer.bukkit.entity;

import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftTameableAnimal;

/**
 * Mohist
 *
 * @author Malcolm - m1lc0lm
 * @Created at 20.02.2022 - 20:40 GMT+1
 * © Copyright 2021 / 2022 - M1lcolm
 */
public class YouerModsTameableEntity extends CraftTameableAnimal {

    public YouerModsTameableEntity(CraftServer server, TamableAnimal entity) {
        super(server, entity);
    }


    @Override
    public TamableAnimal getHandle() {
        return (TamableAnimal) entity;
    }

    @Override
    public String toString() {
        return "YouerCustomTameableAnimal{" + getType() + '}';
    }
}
