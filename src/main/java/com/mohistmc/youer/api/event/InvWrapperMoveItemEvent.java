package com.mohistmc.youer.api.event;

import com.google.common.base.Preconditions;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2023/10/10 3:29:10
 */
public class InvWrapperMoveItemEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Inventory inventory;
    private final ItemStack itemStack;
    private boolean cancelled;

    public InvWrapperMoveItemEvent(final Inventory inventory, final ItemStack itemStack) {
        Preconditions.checkArgument(itemStack != null, "ItemStack cannot be null");
        this.inventory = inventory;
        this.itemStack = itemStack;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public ItemStack getItem() {
        return itemStack.clone();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static class Extract extends InvWrapperMoveItemEvent implements Cancellable {

        private static final HandlerList handlers = new HandlerList();
        private boolean cancelled;

        public Extract(Inventory inventory, ItemStack itemStack) {
            super(inventory, itemStack);
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }
    }

    public static class Insert extends InvWrapperMoveItemEvent implements Cancellable {

        private static final HandlerList handlers = new HandlerList();
        private boolean cancelled;

        public Insert(Inventory inventory, ItemStack itemStack) {
            super(inventory, itemStack);
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }
    }
}
