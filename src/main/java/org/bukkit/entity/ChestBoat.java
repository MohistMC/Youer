package org.bukkit.entity;

import io.papermc.paper.loottable.LootableEntityInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.Lootable;

/**
 * A {@link Boat} with a chest.
 */
public interface ChestBoat extends Boat, InventoryHolder, LootableEntityInventory {
}
