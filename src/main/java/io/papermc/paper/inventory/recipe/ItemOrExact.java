package io.papermc.paper.inventory.recipe;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public sealed interface ItemOrExact permits ItemOrExact.Item, ItemOrExact.Exact {

    int getMaxStackSize();

    boolean matches(ItemStack stack);

    record Item(Holder<net.minecraft.world.item.Item> item) implements ItemOrExact {

        public Item(final ItemStack stack) {
            this(stack.typeHolder());
        }

        @Override
        public int getMaxStackSize() {
            return this.item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
        }

        @Override
        public boolean matches(final ItemStack stack) {
            return stack.is(this.item);
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof final Item otherItem)) return false;
            return this.item.equals(otherItem.item());
        }

        @Override
        public int hashCode() {
            return this.item.hashCode();
        }
    }

    record Exact(ItemStack stack) implements ItemOrExact {

        @Override
        public int getMaxStackSize() {
            return this.stack.getMaxStackSize();
        }

        @Override
        public boolean matches(final ItemStack stack) {
            return ItemStack.isSameItemSameComponents(this.stack, stack);
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof final Exact otherExact)) return false;
            return ItemStack.isSameItemSameComponents(this.stack, otherExact.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }
    }
}
