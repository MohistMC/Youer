package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * @author Mgazul by MohistMC
 */
public class BanEffect {

    public static boolean check(String effectKey) {
        if (!YouerConfig.ban_effect_enable) return false;
        var list = BanConfig.getListByType(BanType.EFFECT);
        if (list.isEmpty()) return false;
        return list.contains(effectKey);
    }

    public static boolean check(Holder<MobEffect> effect) {
        return check(effect.getRegisteredName());
    }
}
