/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2025.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.youer.bukkit.inventory;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerArmorInvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * @author Mgazul
 * @date 2020/4/10 13:39
 */
public class InventoryOwner {

    private static final AtomicReference<org.bukkit.event.inventory.InventoryCloseEvent.Reason> close$Reason = new AtomicReference<>(org.bukkit.event.inventory.InventoryCloseEvent.Reason.UNKNOWN);

    public static org.bukkit.event.inventory.InventoryCloseEvent.Reason getClose$Reason() {
        return close$Reason.getAndSet(org.bukkit.event.inventory.InventoryCloseEvent.Reason.UNKNOWN);
    }

    public static void setClose$Reason(org.bukkit.event.inventory.InventoryCloseEvent.Reason reason) {
        close$Reason.set(reason);
    }

    public static Inventory getInventory(Container inventory) {
        InventoryHolder owner = get(inventory);
        return (owner == null ? new CraftCustomInventory(inventory).getInventory() : owner.getInventory());
    }

    public static InventoryHolder get(BlockEntity te) {
        return get(te.getLevel(), te.getBlockPos());
    }

    public static InventoryHolder get(Container inventory) {
        try {
            return inventory.getOwner();
        } catch (AbstractMethodError | NullPointerException e) {
            return (inventory instanceof BlockEntity blockEntity) ? get(blockEntity) : null;
        }
    }

    public static InventoryHolder get(Level world, BlockPos pos) {
        if (world == null) return null;
        // Spigot start
        org.bukkit.block.Block block = CraftBlock.at(world, pos);
        if (block == null) {
            return null;
        }
        // Spigot end
        org.bukkit.block.BlockState state = block.getState();
        if (state instanceof InventoryHolder) {
            return (InventoryHolder) state;
        } else if (state instanceof CraftBlockEntityState<? extends BlockEntity> blockEntityState) {
            BlockEntity te = blockEntityState.getTileEntity();
            if (te instanceof Container container) {
                return new CraftCustomInventory(container);
            }
        }
        return null;
    }

    @Nullable
    public static InventoryHolder get(IItemHandler handler) {
        switch (handler) {
            case null -> {
                return null;
            }
            case ItemStackHandler itemStackHandler -> {
                return new CraftCustomInventory(itemStackHandler);
            }
            case SlotItemHandler slotItemHandler -> {
                return new CraftCustomInventory(slotItemHandler.container);
            }
            case InvWrapper invWrapper -> {
                return new CraftCustomInventory(invWrapper.getInv());
            }
            case SidedInvWrapper sidedInvWrapper -> {
                return new CraftCustomInventory(sidedInvWrapper.inv);
            }
            case PlayerInvWrapper playerInvWrapper -> {
                IItemHandlerModifiable[] piw = playerInvWrapper.itemHandler;
                for (IItemHandlerModifiable itemHandler : piw) {
                    if (itemHandler instanceof PlayerMainInvWrapper) {
                        return new CraftCustomInventory(((PlayerMainInvWrapper) itemHandler).getInventoryPlayer());
                    }
                    if (itemHandler instanceof PlayerArmorInvWrapper) {
                        return new CraftCustomInventory(((PlayerArmorInvWrapper) itemHandler).getInventoryPlayer());
                    }
                }
            }
            default -> {
            }
        }
        return null;
    }

    @Nullable
    public static Inventory inventoryFromForge(IItemHandler handler) {
        InventoryHolder holder = get(handler);
        return holder != null ? holder.getInventory() : null;
    }

}
