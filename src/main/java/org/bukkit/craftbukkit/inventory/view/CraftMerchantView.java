package org.bukkit.craftbukkit.inventory.view;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.view.MerchantView;
import org.jetbrains.annotations.NotNull;

public class CraftMerchantView extends CraftInventoryView<MerchantMenu, MerchantInventory> implements MerchantView {

    private final net.minecraft.world.item.trading.Merchant trader;

    public CraftMerchantView(final HumanEntity player, final MerchantInventory viewing, final MerchantMenu container, final Merchant trader) {
        super(player, viewing, container);
        this.trader = trader;
    }

    @Override
    public org.bukkit.inventory.@NotNull Merchant getMerchant() {
        return this.trader.getCraftMerchant();
    }
}
