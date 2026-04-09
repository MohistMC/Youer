package com.mohistmc.youer.api.event.block;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public class BlockSetBlockEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Location sourceLocation;
    private final Location location;
    private boolean cancel;

    public BlockSetBlockEvent(Location sourceLocation, Location location) {
        super(location.getBlock());
        this.sourceLocation = sourceLocation;
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

    public Location getSourceLocation() {
        return sourceLocation;
    }
}
