package org.bukkit.entity;

import com.destroystokyo.paper.entity.RangedEntity;

/**
 * Represents a snowman entity
 */
public interface Snowman extends Golem, RangedEntity, io.papermc.paper.entity.Shearable { // Paper

    /**
     * Gets whether this snowman is in "derp mode", meaning it is not wearing a
     * pumpkin.
     *
     * @return True if the snowman is bald, false if it is wearing a pumpkin
     */
    boolean isDerp();

    /**
     * Sets whether this snowman is in "derp mode", meaning it is not wearing a
     * pumpkin. NOTE: This value is not persisted to disk and will therefore
     * reset when the chunk is reloaded.
     *
     * @param derpMode True to remove the pumpkin, false to add a pumpkin
     */
    void setDerp(boolean derpMode);

    // Purpur start
    /**
     * Get the player that summoned this snowman
     *
     * @return UUID of summoner
     */
    @org.jetbrains.annotations.Nullable java.util.UUID getSummoner();

    /**
     * Set the player that summoned this snowman
     *
     * @param summoner UUID of summoner
     */
    void setSummoner(@org.jetbrains.annotations.Nullable java.util.UUID summoner);
    // Purpur end
}
