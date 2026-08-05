package com.mohistmc.youer.feature;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2026/8/6
 */
public class EntityClearInventory implements InventoryHolder {

    private final Inventory inventory;
    @Getter
    private final EntityClearType type;

    public EntityClearInventory(EntityClearType type, String title) {
        this.inventory = Bukkit.createInventory(this, 54, title);
        this.type = type;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
