package org.bukkit.block;

import java.util.List;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a captured state of a block which stores entities.
 *
 * @param <T> Entity this block can store
 */
public interface EntityBlockStorage<T extends Entity> extends TileState {

    /**
     * Check if the block is completely full of entities.
     *
     * @return True if block is full
     */
    boolean isFull();

    /**
     * Get the amount of entities currently in this block.
     *
     * @return Amount of entities currently in this block
     */
    int getEntityCount();

    /**
     * Get the maximum amount of entities this block can hold.
     *
     * @return Maximum amount of entities this block can hold
     */
    int getMaxEntities();

    /**
     * Set the maximum amount of entities this block can hold.
     *
     * @param max Maximum amount of entities this block can hold
     */
    void setMaxEntities(int max);

    /**
     * Release all the entities currently stored in the block.
     *
     * @return List of all entities which were released
     */
    @NotNull
    List<T> releaseEntities();

    // Purpur start
    /**
     * Releases a stored entity, and returns the entity in the world.
     *
     * @param entity Entity to release
     * @return The entity which was released, or null if the stored entity is not in the hive
     */
    @org.jetbrains.annotations.Nullable
    T releaseEntity(@NotNull org.purpurmc.purpur.entity.StoredEntity<T> entity);

    /**
     * Gets all the entities currently stored in the block.
     *
     * @return List of all entities which are stored in the block
     */
    @NotNull
    List<org.purpurmc.purpur.entity.StoredEntity<T>> getEntities();
    //Purpur end
    /**
     * Add an entity to the block.
     *
     * @param entity Entity to add to the block
     */
    void addEntity(@NotNull T entity);

    // Paper start - Add EntityBlockStorage clearEntities
    /**
     * Clear all currently stored entities in the block.
     */
    void clearEntities();
    // Paper end
}
