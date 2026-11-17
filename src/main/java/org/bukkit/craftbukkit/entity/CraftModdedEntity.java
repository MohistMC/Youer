package org.bukkit.craftbukkit.entity;

import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.CraftServer;

/**
 * Youer - mod compat: generic Bukkit view of an entity whose type was registered by a mod.
 * <p>
 * Bukkit's {@link org.bukkit.entity.EntityType} is a fixed enum, so entity types added by mods have no craft type data
 * and no subclass of {@link CraftEntity} to convert to. Reporting them as {@link org.bukkit.entity.EntityType#UNKNOWN} keeps
 * Bukkit events (and the plain Entity API) usable, while this wrapper keeps
 * {@link CraftEntity#getEntity} from aborting the server with an {@code AssertionError} the moment a mod spawns or
 * loads one of its own entities.
 */
public class CraftModdedEntity extends CraftEntity {

    public CraftModdedEntity(final CraftServer server, final Entity entity) {
        super(server, entity);
    }
}
