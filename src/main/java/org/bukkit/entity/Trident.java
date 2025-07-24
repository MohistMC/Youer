package org.bukkit.entity;

/**
 * Represents a thrown trident.
 */
// Paper start
public interface Trident extends AbstractArrow, ThrowableProjectile {

    /**
     * Gets if this trident has dealt damage to an
     * entity yet or has hit the floor.
     *
     * If neither of these events have occurred yet, this will
     * return false.
     *
     * @return has dealt damage
     */
    boolean hasDealtDamage();

    /**
     * Sets if this trident has dealt damage to an entity
     * yet or has hit the floor.
     *
     * @param hasDealtDamage has dealt damage or hit the floor
     */
    void setHasDealtDamage(boolean hasDealtDamage);

    /**
     * Sets the base amount of damage this trident will do.
     *
     * @param damage new damage amount
     */
    void setDamage(double damage);

    /**
     * Gets the base amount of damage this trident will do.
     *
     * Defaults to 8.0 for a normal trident with
     * <code>0.5 * (1 + power level)</code> added for trident fired from
     * damage enchanted bows.
     *
     * @return base damage amount
     */
    double getDamage();
}
// Paper end