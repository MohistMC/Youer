package io.papermc.paper.event.inventory;

import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

@DefaultQualifier(NonNull.class)
public class PaperInventoryMoveItemEvent extends InventoryMoveItemEvent {

    public boolean calledSetItem;
    public boolean calledGetItem;

    public PaperInventoryMoveItemEvent(final @NotNull Inventory sourceInventory, final @NotNull ItemStack itemStack, final @NotNull Inventory destinationInventory, final boolean didSourceInitiate) {
        super(sourceInventory, itemStack, destinationInventory, didSourceInitiate);
    }

    @Override
    public ItemStack getItem() {
        this.calledGetItem = true;
        return super.getItem();
    }

    @Override
    public void setItem(final ItemStack itemStack) {
        super.setItem(itemStack);
        this.calledSetItem = true;
    }
}
