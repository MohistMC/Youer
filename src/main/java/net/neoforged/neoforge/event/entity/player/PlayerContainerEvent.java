/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event.entity.player;

import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class PlayerContainerEvent extends PlayerEvent {
    private final AbstractContainerMenu container;
    @Getter
    private final org.bukkit.event.inventory.InventoryCloseEvent.Reason close$Reason;

    public PlayerContainerEvent(Player player, AbstractContainerMenu container) {
        super(player);
        this.container = container;
        this.close$Reason = org.bukkit.event.inventory.InventoryCloseEvent.Reason.UNKNOWN;
    }

    public PlayerContainerEvent(Player player, AbstractContainerMenu container, org.bukkit.event.inventory.InventoryCloseEvent.Reason close$Reason) {
        super(player);
        this.container = container;
        this.close$Reason = close$Reason;
    }

    public static class Open extends PlayerContainerEvent {
        public Open(Player player, AbstractContainerMenu container) {
            super(player, container);
        }
    }

    public static class Close extends PlayerContainerEvent {
        public Close(Player player, AbstractContainerMenu container) {
            super(player, container);
        }
        public Close(Player player, AbstractContainerMenu container, org.bukkit.event.inventory.InventoryCloseEvent.Reason close$Reason) {
            super(player, container, close$Reason);
        }
    }

    public AbstractContainerMenu getContainer() {
        return container;
    }
}
