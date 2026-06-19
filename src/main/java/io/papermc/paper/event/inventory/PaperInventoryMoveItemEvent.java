package io.papermc.paper.event.inventory;

import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PaperInventoryMoveItemEvent extends InventoryMoveItemEvent {

    public boolean calledSetItem;
    public boolean calledGetItem;

    public PaperInventoryMoveItemEvent(final Inventory sourceInventory, final ItemStack itemStack, final Inventory destinationInventory, final boolean didSourceInitiate) {
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
