package com.mohistmc.youer.util;

import net.minecraft.world.entity.Entity;

public class LambdaFix {

    public static boolean checkBelowWorld(Entity entity) {
        return entity.level().paperConfig().environment.netherCeilingVoidDamageHeight.test(v -> entity.getY() >= v);
    }
}
