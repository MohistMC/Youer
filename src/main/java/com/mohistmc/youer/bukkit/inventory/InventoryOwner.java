/*
 * Copyright (C) MohistMC.
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

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * @author Mgazul
 * {@code @date} 2020/4/10 13:39
 */
public class InventoryOwner {

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
            BlockEntity te = blockEntityState.getBlockEntity();
            if (te instanceof Container container) {
                return new CraftCustomInventory(container);
            }
        }
        return null;
    }

    @Nullable
    public static InventoryHolder get(ResourceHandler<ItemResource> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof PlayerInventoryWrapper playerInvWrapper) {
            return new CraftCustomInventory(playerInvWrapper.getInventory());
        }
        if (handler instanceof VanillaContainerWrapper containerWrapper) {
            return new CraftCustomInventory(containerWrapper.getContainer());
        }
        if (handler instanceof WorldlyContainerWrapper containerWrapper) {
            return new CraftCustomInventory(containerWrapper.getContainer());
        }
        if (handler instanceof ResourceHandlerSlot slot) {
            return get(slot.getResourceHandler());
        }
        // Generic fallback: wrap the handler into a Bukkit inventory.
        return new CraftCustomInventory(new YouerIItemHandlerInventory(handler, null));
    }

    public static Container getContainer(ResourceHandler<?> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof VanillaContainerWrapper wrapper) {
            return wrapper.getContainer();
        }
        if (handler instanceof WorldlyContainerWrapper wrapper) {
            return wrapper.getContainer();
        }
        if (handler instanceof ResourceHandlerSlot slot) {
            return getContainer(slot.getResourceHandler());
        }
        if (handler instanceof DelegatingResourceHandler<?> delegating) {
            return getContainer(delegating.getDelegate());
        }
        return null;
    }

    @Nullable
    public static Inventory inventoryFromForge(ResourceHandler<ItemResource> handler) {
        InventoryHolder holder = get(handler);
        return holder != null ? holder.getInventory() : null;
    }

    public static Inventory getOwnerInventory(Object nmsOwner, ResourceHandler<ItemResource> handler) {
        Container nms = getContainer(handler);
        if (nms != null) {
            final var inventory = nms.getOwnerInventory();
            if (inventory != null) {
                return inventory;
            }
        }
        return new CraftInventory(new YouerIItemHandlerInventory(handler, nmsOwner));
    }

}
