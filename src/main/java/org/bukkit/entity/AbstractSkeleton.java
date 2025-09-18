package org.bukkit.entity;

import com.destroystokyo.paper.entity.RangedEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * This interface defines or represents the abstract concept of skeleton-like
 * entities on the server. The interface is hence not a direct representation
 * of an entity but rather serves as a parent to interfaces/entity types like
 * {@link Skeleton}, {@link WitherSkeleton} or {@link Stray}.
 *
 * To compute what specific type of skeleton is present in a variable/field
 * of this type, instanceOf checks against the specific subtypes listed prior
 * are recommended.
 */
public interface AbstractSkeleton extends Monster, RangedEntity { // Paper

    /**
     * Gets the current type of this skeleton.
     *
     * @return Current type
     * @deprecated should check what class instance this is.
     */
    @Deprecated
    @NotNull
    public Skeleton.SkeletonType getSkeletonType();

    /**
     * @param type type
     * @deprecated Must spawn a new subtype variant
     */
    @Deprecated
    @Contract("_ -> fail")
    public void setSkeletonType(Skeleton.SkeletonType type);

    // Paper start
    /**
     * Check if this skeleton will burn in the sunlight. This
     * does not take into account an entity's natural fire
     * immunity.
     *
     * @return True if skeleton will burn in sunlight
     */
    boolean shouldBurnInDay();

    /**
     * Set if this skeleton should burn in the sunlight. This
     * will not override an entity's natural fire
     * immunity.
     *
     * @param shouldBurnInDay True to burn in sunlight
     */
    void setShouldBurnInDay(boolean shouldBurnInDay);
    // Paper end
}
