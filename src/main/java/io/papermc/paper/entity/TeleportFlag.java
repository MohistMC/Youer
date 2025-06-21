package io.papermc.paper.entity;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Represents a flag that can be set on teleportation that may
 * slightly modify the behavior.
 *
 * @see EntityState
 * @see Relative
 */
public sealed interface TeleportFlag permits TeleportFlag.EntityState, TeleportFlag.Relative {

    /**
     * Note: These flags only work on {@link org.bukkit.entity.Player} entities.
     * <p>
     * Represents coordinates in a teleportation that should be handled relatively.
     * <p>
     * Coordinates of the location that the client should handle as relative teleportation
     * Relative teleportation flags are only used client side, and cause the player to not lose velocity in that
     * specific coordinate. The location of the teleportation will not change.
     *
     * @see org.bukkit.entity.Player#teleport(Location, PlayerTeleportEvent.TeleportCause, TeleportFlag...)
     */
    enum Relative implements TeleportFlag {
        /**
         * Represents the player's X coordinate
         */
        X,
        /**
         * Represents the player's Y coordinate
         */
        Y,
        /**
         * Represents the player's Z coordinate
         */
        Z,
        /**
         * Represents the player's yaw
         */
        YAW,
        /**
         * Represents the player's pitch
         */
        PITCH;
    }

    /**
     * Represents flags that effect the entity's state on
     * teleportation.
     */
    enum EntityState implements TeleportFlag {
        /**
         * If all passengers should not be required to be removed prior to teleportation.
         * <p>
         * Note:
         * Teleporting to a different world with this flag present while the entity has entities riding it
         * will cause this teleportation to return false and not occur.
         */
        RETAIN_PASSENGERS,
        /**
         * If the entity should not be dismounted if they are riding another entity.
         * <p>
         * Note:
         * Teleporting to a different world with this flag present while this entity is riding another entity will
         * cause this teleportation to return false and not occur.
         */
        RETAIN_VEHICLE,
        /**
         * Indicates that a player should not have their current open inventory closed when teleporting.
         * <p>
         * Note:
         * This option will be ignored when teleported to a different world.
         */
        RETAIN_OPEN_INVENTORY;
    }

}
