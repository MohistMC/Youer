package com.mohistmc.youer.api.event;

import lombok.Getter;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2025/11/1 17:47
 */
public class ClientboundOpenScreenEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final Player player;
    @Getter
    private final AbstractContainerMenu menu;
    private boolean cancel;

    public ClientboundOpenScreenEvent(Player player, AbstractContainerMenu menu) {
        this.player = player;
        this.menu = menu;
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
