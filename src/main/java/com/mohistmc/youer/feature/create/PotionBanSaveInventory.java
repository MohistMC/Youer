package com.mohistmc.youer.feature.create;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Add-GUI for /create_item_drain potionban add: players place potion items in it, recognized on close.
 *
 * @author Mgazul
 * @date 2026/8/7
 */
public class PotionBanSaveInventory implements InventoryHolder {

    private final Inventory inventory;

    public PotionBanSaveInventory(String title) {
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
