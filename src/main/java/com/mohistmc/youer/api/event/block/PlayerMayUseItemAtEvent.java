package com.mohistmc.youer.api.event.block;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2025/11/6 15:21
 */
public class PlayerMayUseItemAtEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Location location;
    private final ItemStack itemStack;
    private boolean cancel;

    public PlayerMayUseItemAtEvent(Player player, Location location, ItemStack itemStack) {
        super(player);
        this.itemStack = itemStack;
        this.location = location;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
