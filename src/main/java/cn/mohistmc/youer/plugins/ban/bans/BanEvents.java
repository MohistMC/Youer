package cn.mohistmc.youer.plugins.ban.bans;

import cn.mohistmc.youer.YouerConfig;

/**
 * @author Mgazul by MohistMC
 * @date 2023/8/9 20:09:51
 */
public class BanEvents {

    public static boolean banFireTick() {
        return YouerConfig.doFireTick;
    }
}
