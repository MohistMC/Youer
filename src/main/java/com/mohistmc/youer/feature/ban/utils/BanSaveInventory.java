package com.mohistmc.youer.feature.ban.utils;

import com.mohistmc.youer.feature.ban.BanType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BanSaveInventory implements InventoryHolder {

    private final Inventory inventory;
    @Getter
    private final BanType banType;

    public BanSaveInventory(BanType banType, String title) {
        this.inventory = Bukkit.createInventory(this, 54, title);
        this.banType = banType;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

}
