package org.bukkit.entity;

import io.papermc.paper.entity.RangedEntity;

/**
 * Represents a Witch
 */
public interface Witch extends Raider, RangedEntity { // Paper

    /**
     * Gets whether the witch is drinking a potion
     *
     * @return whether the witch is drinking a potion
     */
    boolean isDrinkingPotion();
}
