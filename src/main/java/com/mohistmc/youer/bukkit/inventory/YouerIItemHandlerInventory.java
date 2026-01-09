package com.mohistmc.youer.bukkit.inventory;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

/**
 * @author Mgazul
 * @date 2025/11/30 01:30
 */
public class YouerIItemHandlerInventory implements Container {

    @Nonnull
    private final IItemHandler delegate;

    @Nullable
    private final Container original;

    @Nullable
    private final Object nmsOwner;

    private final List<HumanEntity> transaction = new ArrayList<>();

    public YouerIItemHandlerInventory(@Nonnull IItemHandler delegate, @Nullable Object nmsOwner) {
        this.nmsOwner = nmsOwner;
        this.delegate = delegate;
        this.original = InventoryOwner.getContainer(delegate);
    }

    public YouerIItemHandlerInventory(@Nonnull Pair<IItemHandler, Object> input) {
        this(input.getLeft(), input.getRight());
    }
    @Override
    public int getContainerSize() {
        return delegate.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < delegate.getSlots(); i++) {
            if (!delegate.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int p_18941_) {
        return delegate.getStackInSlot(p_18941_).copy();
    }

    @Override
    public ItemStack removeItem(int p_18942_, int p_18943_) {
        return delegate.extractItem(p_18942_, p_18943_, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_18951_) {
        return delegate.extractItem(p_18951_, Integer.MAX_VALUE, false);
    }

    @Override
    public void setItem(int p_18944_, ItemStack p_18945_) {
        if (!delegate.isItemValid(p_18944_, p_18945_)) {
            return;
        }
        final var content = getItem(p_18944_);
        final var take = delegate.extractItem(p_18944_, Integer.MAX_VALUE, true);
        if (take.getCount() != content.getCount()) {
            return;
        }
        final var raw = delegate.extractItem(p_18944_, Integer.MAX_VALUE, false);
        if (delegate.insertItem(p_18944_, p_18945_, true).isEmpty()) {
            delegate.insertItem(p_18944_, p_18945_, false);
            return;
        }
        delegate.insertItem(p_18944_, raw, false);
    }

    @Override
    public int getMaxStackSize() {
        int maxStack = 0;
        for (int i = 0; i < delegate.getSlots(); i++) {
            final int limit = delegate.getSlotLimit(i);
            if (limit > maxStack) {
                maxStack = limit;
            }
        }
        return maxStack;
    }

    @Override
    public void setChanged() {
        if (original != null) {
            original.setChanged();
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                be.setChanged();
            }
        }
    }

    @Override
    public boolean stillValid(Player p_18946_) {
        if (original != null) {
            return original.stillValid(p_18946_);
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                return Container.stillValidBlockEntity(be, p_18946_);
            } else if (nmsOwner instanceof Entity entity) {
                return p_18946_.canInteractWithEntity(entity, 4.0F);
            }
        }
        return true;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack arg) {
        return delegate.isItemValid(i, arg);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < delegate.getSlots(); i++) {
            delegate.extractItem(i, Integer.MAX_VALUE, false);
        }
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public InventoryHolder getOwner() {
        if (original != null) {
            return original.getOwner();
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                return be.getOwner(); // BlockEntity
            } else if (nmsOwner instanceof Entity entity) {
                return entity.getBukkitEntity() instanceof InventoryHolder result ? result : null; // Entity
            }
        }
        return null;
    }

    @Override
    public void setMaxStackSize(int size) {
    }

    @Override
    public Location getLocation() {
        if (original != null) {
            return original.getLocation();
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                return CraftLocation.toBukkit(be.getBlockPos(), be.getLevel());
            } else if (nmsOwner instanceof Entity entity) {
                return CraftLocation.toBukkit(entity.position(), entity.level().getWorld());
            }
        }
        return null;
    }
}
