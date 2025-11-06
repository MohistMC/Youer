package com.mohistmc.youer.api.event.block;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2025/11/6 14:30
 */
public class PlayerMayInteractBlockEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player entity;
    @Getter
    private final Location location;
    private boolean cancel;

    public PlayerMayInteractBlockEvent(Player entity, Location location) {
        super(location.getBlock());
        this.location = location;
        this.entity = entity;
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
}
