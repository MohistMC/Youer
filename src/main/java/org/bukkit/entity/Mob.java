package org.bukkit.entity;

import org.bukkit.Sound;
import org.bukkit.loot.Lootable;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Mob. Mobs are living entities with simple AI.
 */
public interface Mob extends LivingEntity, Lootable, io.papermc.paper.entity.Leashable { // Paper - Leashable API

    /**
     * Instructs this Mob to set the specified LivingEntity as its target.
     * <p>
     * Hostile creatures may attack their target, and friendly creatures may
     * follow their target.
     *
     * @param target New LivingEntity to target, or null to clear the target
     */
    public void setTarget(@Nullable LivingEntity target);

    /**
     * Gets the current target of this Mob
     *
     * @return Current target of this creature, or null if none exists
     */
    @Nullable
    public LivingEntity getTarget();

    /**
     * Sets whether this mob is aware of its surroundings.
     *
     * Unaware mobs will still move if pushed, attacked, etc. but will not move
     * or perform any actions on their own. Unaware mobs may also have other
     * unspecified behaviours disabled, such as drowning.
     *
     * @param aware whether the mob is aware
     */
    public void setAware(boolean aware);

    /**
     * Gets whether this mob is aware of its surroundings.
     *
     * Unaware mobs will still move if pushed, attacked, etc. but will not move
     * or perform any actions on their own. Unaware mobs may also have other
     * unspecified behaviours disabled, such as drowning.
     *
     * @return whether the mob is aware
     */
    public boolean isAware();

    /**
     * Get the {@link Sound} this mob makes while ambiently existing. This sound
     * may change depending on the current state of the entity, and may also
     * return null under specific conditions. This sound is not constant.
     * For instance, villagers will make different passive noises depending
     * on whether or not they are actively trading with a player, or make no
     * ambient noise while sleeping.
     *
     * @return the ambient sound, or null if this entity is ambiently quiet
     */
    @Nullable
    public Sound getAmbientSound();

    // Paper start - Missing Entity API
    /**
     * Some mobs will raise their arm(s) when aggressive:
     * <ul>
     *     <li>{@link Drowned}</li>
     *     <li>{@link Piglin}</li>
     *     <li>{@link Skeleton}</li>
     *     <li>{@link Zombie}</li>
     *     <li>{@link ZombieVillager}</li>
     *     <li>{@link Illusioner}</li>
     *     <li>{@link Vindicator}</li>
     *     <li>{@link Panda}</li>
     *     <li>{@link Pillager}</li>
     *     <li>{@link PiglinBrute}</li>
     * </ul>
     * <p>
     * Note: This doesn't always show the actual aggressive state as
     * set by {@link #setAggressive(boolean)}. {@link Panda}'s are always
     * aggressive if their combined {@link Panda.Gene} is {@link Panda.Gene#AGGRESSIVE}.
     *
     * @return wether the mob is aggressive or not
     */
    boolean isAggressive();

    /**
     * Some mobs will raise their arm(s) when aggressive,
     * see {@link #isAggressive()} for full list.
     *
     * @param aggressive wether the mob should be aggressive or not
     * @see #isAggressive()
     */
    void setAggressive(boolean aggressive);
    // Paper end - Missing Entity API

    // Paper start - left-handed API
    /**
     * Check if Mob is left-handed
     *
     * @return True if left-handed
     */
    public boolean isLeftHanded();

    /**
     * Set if Mob is left-handed
     *
     * @param leftHanded True if left-handed
     */
    public void setLeftHanded(boolean leftHanded);
    // Paper end - left-handed API
}
