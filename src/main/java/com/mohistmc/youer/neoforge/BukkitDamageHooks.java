package com.mohistmc.youer.neoforge;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;

public class BukkitDamageHooks {

    /**
     * A large number of lambdas will break the recognition of Mixins, so they need to be kept out separately
     */
    public static org.bukkit.event.entity.EntityDamageEvent handleEntityDamage(LivingEntity livingEntity, final DamageSource damagesource, float f) {
        float originalDamage = f;

        com.google.common.base.Function<Double, Double> freezing = f8 -> {
            if (damagesource.is(DamageTypeTags.IS_FREEZING) && livingEntity.is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                return -(f8 - (f8 * 5.0F));
            }
            return -0.0;
        };
        float freezingModifier = freezing.apply((double) f).floatValue();
        f += freezingModifier;

        com.google.common.base.Function<Double, Double> hardHat = f7 -> {
            if (damagesource.is(DamageTypeTags.DAMAGES_HELMET) && !livingEntity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return -(f7 - (f7 * 0.75F));
            }
            return -0.0;
        };
        float hardHatModifier = hardHat.apply((double) f).floatValue();
        f += hardHatModifier;

        com.google.common.base.Function<Double, Double> blocking = f6 -> -((double) livingEntity.calculateItemBlocking(damagesource, f6.floatValue()));
        float blockingModifier = blocking.apply((double) f).floatValue();
        f += blockingModifier;

        com.google.common.base.Function<Double, Double> armor = f5 -> {
            livingEntity.calDamageEvent.set(true);
            return -(f5 - livingEntity.getDamageAfterArmorAbsorb(damagesource, f5.floatValue()));
        };
        float armorModifier = armor.apply((double) f).floatValue();
        f += armorModifier;

        com.google.common.base.Function<Double, Double> resistance = f4 -> {
            if (!damagesource.is(DamageTypeTags.BYPASSES_EFFECTS) && livingEntity.hasEffect(MobEffects.RESISTANCE) && !damagesource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (livingEntity.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f1 = f4.floatValue() * (float) j;

                return -(f4 - Math.max(f1 / 25.0F, 0.0F));
            }
            return -0.0;
        };
        float resistanceModifier = resistance.apply((double) f).floatValue();
        f += resistanceModifier;

        com.google.common.base.Function<Double, Double> magic = f3 -> -(f3 - livingEntity.getDamageAfterMagicAbsorb(damagesource, f3.floatValue()));
        float magicModifier = magic.apply((double) f).floatValue();
        f += magicModifier;

        com.google.common.base.Function<Double, Double> absorption = f2 -> -(Math.max(f2 - Math.max(f2 - livingEntity.getAbsorptionAmount(), 0.0F), 0.0F));
        float absorptionModifier = absorption.apply((double) f).floatValue();

        return CraftEventFactory.handleLivingEntityDamageEvent(livingEntity, damagesource, originalDamage, freezingModifier, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, freezing, hardHat, blocking, armor, resistance, magic, absorption);
    }
}
