package com.mohistmc.youer.plugins.ban.bans;

import com.mohistmc.youer.MohistConfig;
import com.mohistmc.youer.api.EntityAPI;
import net.minecraft.world.entity.Entity;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 13:35:20
 */
public class BanEntity {

    public static boolean check(Entity entity) {
        if (!MohistConfig.ban_entity_enable) return false;
        return EntityAPI.isBan(entity.getBukkitEntity());
    }
}
