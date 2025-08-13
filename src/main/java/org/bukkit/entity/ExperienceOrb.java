package org.bukkit.entity;

/**
 * Represents an Experience Orb.
 */
public interface ExperienceOrb extends Entity {

    /**
     * Gets how much experience is contained within this orb
     *
     * @return Amount of experience
     */
    public int getExperience();

    /**
     * Sets how much experience is contained within this orb
     *
     * @param value Amount of experience
     */
    public void setExperience(int value);

    // Paper start - expose count
    /**
     * Get the stacked count for this experience orb.
     *
     * @return the count
     */
    int getCount();

    /**
     * Sets the stacked count for this experience orb.
     *
     * @param count the new count
     */
    void setCount(int count);
    // Paper end
}
