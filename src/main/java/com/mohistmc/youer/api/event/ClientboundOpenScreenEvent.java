package com.mohistmc.youer.api.event;

import com.mohistmc.youer.api.MenuAPI;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called on the server thread right before an open-screen packet is sent to the client.
 * <p>
 * Cancelling this event prevents the screen from opening and closes the menu provider,
 * which mirrors the behaviour of a cancelled {@link org.bukkit.event.inventory.InventoryOpenEvent}.
 * <p>
 * Use {@link #isModded()}, {@link #getModId()} and {@link #getModName()} to find out whether
 * the screen comes from a mod.
 *
 * @author Mgazul
 * @date 2025/11/1 17:47
 */
public class ClientboundOpenScreenEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
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

    public AbstractContainerMenu getMenu() {
        return menu;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * @return whether the menu being opened is provided by a mod
     */
    public boolean isModded() {
        return MenuAPI.isModded(menu);
    }

    /**
     * @return whether the menu being opened is vanilla or provided by the server itself
     */
    public boolean isVanilla() {
        return MenuAPI.isVanilla(menu);
    }

    /**
     * @return the mod id owning the menu, or {@code null} when it is vanilla/server provided
     */
    public @Nullable String getModId() {
        return MenuAPI.getModId(menu);
    }

    /**
     * @return the display name of the mod owning the menu, or {@code null} when it is vanilla/server provided
     */
    public @Nullable String getModName() {
        return MenuAPI.getModName(menu);
    }

    /**
     * @return the registry id of the menu type, for example {@code minecraft:generic_9x3}
     */
    public @Nullable String getMenuTypeId() {
        return MenuAPI.getMenuTypeId(menu);
    }

    /**
     * @return the Bukkit inventory type created for this modded menu, or {@code null} for vanilla menus
     */
    public @Nullable InventoryType getInventoryType() {
        return MenuAPI.getInventoryType(menu);
    }
}
