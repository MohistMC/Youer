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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

/**
 * @author Mgazul
 * {@code @date} 2025/11/30 01:30
 */
public class YouerIItemHandlerInventory implements Container {

    @Nonnull
    private final ResourceHandler<ItemResource> delegate;

    @Nullable
    private final Container original;

    @Nullable
    private final Object nmsOwner;

    private final List<HumanEntity> transaction = new ArrayList<>();

    public YouerIItemHandlerInventory(@Nonnull ResourceHandler<ItemResource> delegate, @Nullable Object nmsOwner) {
        this.nmsOwner = nmsOwner;
        this.delegate = delegate;
        this.original = InventoryOwner.getContainer(delegate);
    }

    public YouerIItemHandlerInventory(@Nonnull Pair<ResourceHandler<ItemResource>, Object> input) {
        this(input.getLeft(), input.getRight());
    }
    @Override
    public int getContainerSize() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < delegate.size(); i++) {
            if (!ItemUtil.getStack(delegate, i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NonNull ItemStack getItem(int p_18941_) {
        return ItemUtil.getStack(delegate, p_18941_).copy();
    }

    @Override
    public @NonNull ItemStack removeItem(int p_18942_, int p_18943_) {
        return extract(p_18942_, p_18943_);
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int p_18951_) {
        return extract(p_18951_, Integer.MAX_VALUE);
    }

    private ItemStack extract(int slot, int amount) {
        var resource = delegate.getResource(slot);
        if (resource.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        amount = Math.min(amount, resource.getMaxStackSize());
        try (var tx = Transaction.openRoot()) {
            int extracted = delegate.extract(slot, resource, amount, tx);
            tx.commit();
            return resource.toStack(extracted);
        }
    }

    @Override
    public void setItem(int p_18944_, ItemStack p_18945_) {
        if (!p_18945_.isEmpty() && !delegate.isValid(p_18944_, ItemResource.of(p_18945_))) {
            return;
        }
        try (var tx = Transaction.openRoot()) {
            // Clear the slot contents.
            var current = ItemUtil.getStack(delegate, p_18944_);
            if (!current.isEmpty()) {
                var currentResource = ItemResource.of(current);
                if (delegate.extract(p_18944_, currentResource, current.getCount(), tx) != current.getCount()) {
                    return; // The slot could not be fully cleared; abort.
                }
            }
            if (p_18945_.isEmpty()) {
                tx.commit();
                return;
            }
            // Try to insert the new stack into the cleared slot.
            if (delegate.insert(p_18944_, ItemResource.of(p_18945_), p_18945_.getCount(), tx) == p_18945_.getCount()) {
                tx.commit();
            }
            // If the insert did not fully succeed, the transaction rolls back, restoring the original contents.
        }
    }

    @Override
    public int getMaxStackSize() {
        int maxStack = 0;
        for (int i = 0; i < delegate.size(); i++) {
            final int limit = delegate.getCapacityAsInt(i, ItemResource.EMPTY);
            if (limit > maxStack) {
                maxStack = limit;
            }
        }
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        Container.super.setMaxStackSize(size);
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
                return p_18946_.isWithinEntityInteractionRange(entity, 4.0F);
            }
        }
        return true;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack arg) {
        return arg.isEmpty() || delegate.isValid(i, ItemResource.of(arg));
    }

    @Override
    public void clearContent() {
        try (var tx = Transaction.openRoot()) {
            for (int i = 0; i < delegate.size(); i++) {
                var resource = delegate.getResource(i);
                if (!resource.isEmpty()) {
                    delegate.extract(i, resource, Integer.MAX_VALUE, tx);
                }
            }
            tx.commit();
        }
    }

    @Override
    public void onOpen(@NonNull CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(@NonNull CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public @NonNull List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public @NonNull InventoryHolder getOwner() {
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
