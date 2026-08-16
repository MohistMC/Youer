package com.mohistmc.youer.feature.entitydamage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Weakens the final damage dealt by configured entities.
 *
 * <p>Called directly from {@code LivingEntity#actuallyHurt} right before the
 * target's health is changed, so no other mod or event listener can modify the
 * weakened value afterwards.</p>
 */
public class EntityDamageWeaken {

    private EntityDamageWeaken() {
    }

    public static float weaken(DamageSource source, float damage) {
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return damage;
        }
        Integer percent = EntityDamageConfig.getWeakenPercent(
                BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString());
        if (percent == null) {
            return damage;
        }
        return damage * (100 - percent) / 100f;
    }
}
