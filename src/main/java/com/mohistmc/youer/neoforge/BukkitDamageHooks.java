package com.mohistmc.youer.neoforge;

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
    public static org.bukkit.event.entity.EntityDamageEvent handleEntityDamage(LivingEntity livingEntity, final DamageSource damagesource, float damage, final float invulnerabilityRelatedLastDamage) { // Paper - fix invulnerability reduction in EntityDamageEvent
        float originalDamage = damage;
        // Paper start - fix invulnerability reduction in EntityDamageEvent
        final com.google.common.base.Function<Double, Double> invulnerabilityReductionEquation = mod -> {
            if (invulnerabilityRelatedLastDamage == 0) return 0.0; // no last damage, no reduction
            // last damage existed, this means the reduction *technically* is (new damage - last damage).
            // If the event damage was changed to something less than invul damage, hard lock it at 0.
            //
            // Cast the passed in double down to a float as double -> float -> double is lossy.
            // If last damage is a (float) 3.2 (since the events use doubles), we cannot compare
            // the new damage value of this damage instance by upcasting it again to a double as 3.2 != (double) (float) 3.2.
            if (mod.floatValue() < invulnerabilityRelatedLastDamage) return 0.0;
            return (double) -invulnerabilityRelatedLastDamage;
        };
        final float originalInvulnerabilityReduction = invulnerabilityReductionEquation.apply((double) damage).floatValue();
        damage += originalInvulnerabilityReduction;
        // Paper end - fix invulnerability reduction in EntityDamageEvent

        com.google.common.base.Function<Double, Double> freezing = mod -> {
            if (damagesource.is(DamageTypeTags.IS_FREEZING) && livingEntity.is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                return -(mod - mod * 5.0F);
            }
            return -0.0;
        };
        float freezingModifier = freezing.apply((double) damage).floatValue();
        damage += freezingModifier;

        com.google.common.base.Function<Double, Double> hardHat = mod -> {
            if (damagesource.is(DamageTypeTags.DAMAGES_HELMET) && !livingEntity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return -(mod - mod * 0.75F);
            }
            return -0.0;
        };
        float hardHatModifier = hardHat.apply((double) damage).floatValue();
        damage += hardHatModifier;

        com.google.common.base.Function<Double, Double> blocking = mod -> {
            if (!livingEntity.canBlockAttack(damagesource, mod.floatValue())) {
                return 0D;
            }
            return (double) -livingEntity.resolveBlockedDamage(damagesource, mod.floatValue());
        };
        float blockingModifier = blocking.apply((double) damage).floatValue();
        damage += blockingModifier;

        com.google.common.base.Function<Double, Double> armor = mod -> {
            livingEntity.calDamageEvent.set(true); // Youer
            return -(mod - livingEntity.getDamageAfterArmorAbsorb(damagesource, mod.floatValue()));
        };
        float armorModifier = armor.apply((double) damage).floatValue();
        damage += armorModifier;

        com.google.common.base.Function<Double, Double> resistance = mod -> {
            if (!damagesource.is(DamageTypeTags.BYPASSES_EFFECTS) && livingEntity.hasEffect(MobEffects.RESISTANCE) && !damagesource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int absorbValue = (livingEntity.getEffect(net.minecraft.world.effect.MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                int absorb = 25 - absorbValue;
                float v = mod.floatValue() * (float) absorb;

                return -(mod - Math.max(v / 25.0F, 0.0F));
            }
            return -0.0;
        };
        float resistanceModifier = resistance.apply((double) damage).floatValue();
        damage += resistanceModifier;

        com.google.common.base.Function<Double, Double> magic = mod -> -(mod - livingEntity.getDamageAfterMagicAbsorb(damagesource, mod.floatValue()));
        float magicModifier = magic.apply((double) damage).floatValue();
        damage += magicModifier;

        com.google.common.base.Function<Double, Double> absorption = mod -> -(Math.max(mod - Math.max(mod - livingEntity.getAbsorptionAmount(), 0.0F), 0.0F));
        float absorptionModifier = absorption.apply((double) damage).floatValue();

        // Paper start - fix invulnerability reduction in EntityDamageEvent
        return CraftEventFactory.handleLivingEntityDamageEvent(livingEntity, damagesource, originalDamage, freezingModifier, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, freezing, hardHat, blocking, armor, resistance, magic, absorption, (damageModifierDoubleMap, damageModifierFunctionMap) -> {
            damageModifierFunctionMap.put(EntityDamageEvent.DamageModifier.INVULNERABILITY_REDUCTION, invulnerabilityReductionEquation);
            damageModifierDoubleMap.put(EntityDamageEvent.DamageModifier.INVULNERABILITY_REDUCTION, (double) originalInvulnerabilityReduction);
        });
        // Paper end - fix invulnerability reduction in EntityDamageEvent
    }
}
