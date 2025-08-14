package org.bukkit.entity;

/**
 * An iron Golem that protects Villages.
 */
public interface IronGolem extends Golem {

    /**
     * Gets whether this iron golem was built by a player.
     *
     * @return Whether this iron golem was built by a player
     */
    public boolean isPlayerCreated();

    /**
     * Sets whether this iron golem was built by a player or not.
     *
     * @param playerCreated true if you want to set the iron golem as being
     *     player created, false if you want it to be a natural village golem.
     */
    public void setPlayerCreated(boolean playerCreated);

    // Purpur start
    /**
     * Get the player that summoned this iron golem
     *
     * @return UUID of summoner
     */
    @org.jetbrains.annotations.Nullable java.util.UUID getSummoner();

    /**
     * Set the player that summoned this iron golem
     *
     * @param summoner UUID of summoner
     */
    void setSummoner(@org.jetbrains.annotations.Nullable java.util.UUID summoner);
    // Purpur end
}
