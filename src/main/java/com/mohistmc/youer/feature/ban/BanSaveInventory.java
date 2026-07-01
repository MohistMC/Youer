package com.mohistmc.youer.feature.ban;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class BanSaveInventory implements InventoryHolder {

    private final Inventory inventory;
    private final BanType banType;

    public BanSaveInventory(BanType banType, String title) {
        this.inventory = Bukkit.createInventory(this, 54, title);
        this.banType = banType;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public BanType getBanType() {
        return banType;
    }

    public void saveToYaml(HumanEntity player, ClickType clickType, List<String> list, BanType banType) {
        BanConfig.saveToYaml(player, clickType, list, banType);
    }
}
