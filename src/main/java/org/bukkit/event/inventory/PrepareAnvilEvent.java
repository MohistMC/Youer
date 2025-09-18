package org.bukkit.event.inventory;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when an item is put in a slot for repair by an anvil.
 */
public class PrepareAnvilEvent extends PrepareResultEvent {

    // Paper - move HandlerList to PrepareInventoryResultEvent

    public PrepareAnvilEvent(@NotNull AnvilView inventory, @Nullable ItemStack result) {
        super(inventory, result);
    }

    @NotNull
    @Override
    public AnvilInventory getInventory() {
        return (AnvilInventory) super.getInventory();
    }

    @NotNull
    @Override
    public AnvilView getView() {
        return (AnvilView) super.getView();
    }

    // Paper - move HandlerList to PrepareInventoryResultEvent
}
