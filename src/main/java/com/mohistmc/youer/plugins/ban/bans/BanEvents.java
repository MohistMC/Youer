package com.mohistmc.youer.plugins.ban.bans;

import com.mohistmc.youer.YouerConfig;

/**
 * @author Mgazul by MohistMC
 * @date 2023/8/9 20:09:51
 */
public class BanEvents {

    public static boolean banFireTick() {
        return YouerConfig.doFireTick;
    }

    public static boolean banExplosion() {
        return YouerConfig.explosion;
    }

    public static boolean banFarmlandTrample() {
        return YouerConfig.farmlandTrample;
    }
}
