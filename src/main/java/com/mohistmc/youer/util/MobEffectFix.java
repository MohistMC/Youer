package com.mohistmc.youer.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Repairs a Direct (unregistered) mob-effect holder that would otherwise crash the entity save. */
public final class MobEffectFix {

    private MobEffectFix() {
    }

    public static boolean isRegistered(Holder<MobEffect> holder) {
        return holder != null && holder.unwrapKey().isPresent();
    }

    /** The registry holder for an effect, or null when it is not registered. */
    public static Holder<MobEffect> referenceOf(Holder<MobEffect> holder) {
        if (holder == null) {
            return null;
        }
        if (holder.unwrapKey().isPresent()) {
            return holder;
        }
        return BuiltInRegistries.MOB_EFFECT.getResourceKey(holder.value())
                .flatMap(BuiltInRegistries.MOB_EFFECT::getHolder)
                .orElse(null);
    }

    /** A copy of the instance with a registry holder, or null when the effect is unregistered. */
    public static MobEffectInstance normalize(MobEffectInstance instance) {
        if (instance == null || isRegistered(instance.getEffect())) {
            return instance;
        }
        Holder<MobEffect> reference = referenceOf(instance.getEffect());
        if (reference == null) {
            return null;
        }
        return new MobEffectInstance(reference, instance.getDuration(), instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon(), instance.hiddenEffect);
    }
}
