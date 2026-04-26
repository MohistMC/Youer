package com.mohistmc.youer.neoforge;

import com.google.common.base.Function;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;

public class BukkitDamageHooks {

    /**
     * A large number of lambdas will break the recognition of Mixins, so they need to be kept out separately
     */
    public static EntityDamageEvent handleEntityDamage(LivingEntity livingEntity, final DamageSource damagesource, float f) {
        float originalDamage = f;

        Function<Double, Double> freezing = new Function<Double, Double>() {
            @Override
            public Double apply(Double f) {
                if (damagesource.is(DamageTypeTags.IS_FREEZING) && livingEntity.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                    return -(f - (f * 5.0F));
                }
                return -0.0;
            }
        };
        float freezingModifier = freezing.apply((double) f).floatValue();
        f += freezingModifier;

        Function<Double, Double> hardHat = new Function<Double, Double>() {
            @Override
            public Double apply(Double f) {
                if (damagesource.is(DamageTypeTags.DAMAGES_HELMET) && !livingEntity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                    return -(f - (f * 0.75F));
                }
                return -0.0;
            }
        };
        float hardHatModifier = hardHat.apply((double) f).floatValue();
        f += hardHatModifier;

        Function<Double, Double> blocking = new Function<Double, Double>() {
            @Override
            public Double apply(Double f) {
                return -((livingEntity.isDamageSourceBlocked(damagesource)) ? f : 0.0);
            }
        };
        float blockingModifier = blocking.apply((double) f).floatValue();
        f += blockingModifier;

        Function<Double, Double> armor = new Function<Double, Double>() {
            @Override
            public Double apply(Double f) {
                livingEntity.calDamageEvent.set(true);
                return -(f - livingEntity.getDamageAfterArmorAbsorb(damagesource, f.floatValue()));
            }
        };
        float armorModifier = armor.apply((double) f).floatValue();
        f += armorModifier;

        Function<Double, Double> resistance = new Function<Double, Double>() {
            @Override
            public Double apply(Double f) {
                if (!damagesource.is(DamageTypeTags.BYPASSES_EFFECTS) && livingEntity.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !damagesource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                    int i = (livingEntity.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                    int j = 25 - i;
                    float f1 = f.floatValue() * (float) j;
                    return -(f - (f1 / 25.0F));
                }
                return -0.0;
            }
        };
        float resistanceModifier = resistance.apply((double) f).floatValue();
        f += resistanceModifier;

        Function<Double, Double> magic = new Function<Double, Double>() {
            @Override
            public Double apply(Double f) {
                return -(f - livingEntity.getDamageAfterMagicAbsorb(damagesource, f.floatValue()));
            }
        };
        float magicModifier = magic.apply((double) f).floatValue();
        f += magicModifier;

        Function<Double, Double> absorption = new Function<Double, Double>() {
            @Override
            public Double apply(Double f) {
                return -(Math.max(f - Math.max(f - livingEntity.getAbsorptionAmount(), 0.0F), 0.0F));
            }
        };
        float absorptionModifier = absorption.apply((double) f).floatValue();

        return CraftEventFactory.handleLivingEntityDamageEvent(livingEntity, damagesource, originalDamage, freezingModifier, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, freezing, hardHat, blocking, armor, resistance, magic, absorption);
    }
}
