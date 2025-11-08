package com.mohistmc.youer.neoforge;

import com.google.common.base.Function;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class BukkitDamageHooks {

    /**
     * A large number of lambdas will break the recognition of Mixins, so they need to be kept out separately
     */
    public static org.bukkit.event.entity.EntityDamageEvent handleEntityDamage(LivingEntity livingEntity, final DamageSource damagesource, float f) {
        float originalDamage = f;
        Function<Double, Double> freezing = f4 -> {
            if (damagesource.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING) && livingEntity.getType().is(net.minecraft.tags.EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                return -(f4 - (f4 * 5.0F));
            }
            return -0.0;
        };
        float freezingModifier = freezing.apply((double) f).floatValue();
        f += freezingModifier;
        Function<Double, Double> hardHat = f5 -> {
            if (damagesource.is(net.minecraft.tags.DamageTypeTags.DAMAGES_HELMET) && !livingEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()) {
                return -(f5 - (f5 * 0.75F));
            }
            return -0.0;
        };
        float hardHatModifier = hardHat.apply((double) f).floatValue();
        f += hardHatModifier;
        Function<Double, Double> blocking = f6 -> -((livingEntity.isDamageSourceBlocked(damagesource)) ? f6 : 0.0);
        float blockingModifier = blocking.apply((double) f).floatValue();
        f += blockingModifier;
        Function<Double, Double> armor = f7 -> -(f7 - livingEntity.getDamageAfterArmorAbsorb(damagesource, f7.floatValue()));
        float armorModifier = armor.apply((double) f).floatValue();
        f += armorModifier;
        Function<Double, Double> resistance = f8 -> {
            if (!damagesource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS) && livingEntity.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE) && !damagesource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (livingEntity.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f1 = f8.floatValue() * (float) j;
                return -(f8 - (f1 / 25.0F));
            }
            return -0.0;
        };
        float resistanceModifier = resistance.apply((double) f).floatValue();
        f += resistanceModifier;
        com.google.common.base.Function<Double, Double> magic = f9 -> -(f9 - livingEntity.getDamageAfterMagicAbsorb(damagesource, f9.floatValue()));
        float magicModifier = magic.apply((double) f).floatValue();
        f += magicModifier;
        com.google.common.base.Function<Double, Double> absorption = f10 -> -(Math.max(f10 - Math.max(f10 - livingEntity.getAbsorptionAmount(), 0.0F), 0.0F));
        float absorptionModifier = absorption.apply((double) f).floatValue();

        return org.bukkit.craftbukkit.event.CraftEventFactory.handleLivingEntityDamageEvent(livingEntity, damagesource, originalDamage, freezingModifier, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, freezing, hardHat, blocking, armor, resistance, magic, absorption);
    }
}
