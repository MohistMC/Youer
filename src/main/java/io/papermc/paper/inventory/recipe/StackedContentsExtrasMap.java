package io.papermc.paper.inventory.recipe;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public final class StackedContentsExtrasMap {

    private final StackedContents<ItemOrExact> contents;
    public Object2IntMap<ItemOrExact.Item> regularRemoved = new Object2IntOpenHashMap<>(); // needed for re-using the regular contents (for ShapelessRecipe)
    public final ObjectSet<ItemStack> exactIngredients = new ObjectOpenCustomHashSet<>(ItemStackLinkedSet.TYPE_AND_TAG);

    public StackedContentsExtrasMap(final StackedContents<ItemOrExact> contents) {
        this.contents = contents;
    }

    public void initialize(final Recipe<?> recipe) {
        this.exactIngredients.clear();
        for (final Ingredient ingredient : recipe.placementInfo().ingredients()) {
            if (ingredient.isExact()) {
                this.exactIngredients.addAll(ingredient.itemStacks());
            }
        }
    }

    public void accountInput(final CraftingInput input) {
        // similar logic to the CraftingInput constructor
        for (final ItemStack item : input.items()) {
            if (!item.isEmpty()) {
                if (this.accountStack(item, 1)) {
                    // if stack was accounted for as an exact ingredient, don't include it in the regular contents
                    final ItemOrExact.Item asItem = new ItemOrExact.Item(item);
                    if (this.contents.amounts.containsKey(asItem)) {
                        final int amount = this.contents.amounts.removeInt(asItem);
                        this.regularRemoved.put(asItem, amount);
                    }
                }
            }
        }
    }

    public void resetExtras() {
        // clear previous extra ids
        for (final ItemStack extra : this.exactIngredients) {
            this.contents.amounts.removeInt(new ItemOrExact.Exact(extra));
        }
        for (final Object2IntMap.Entry<ItemOrExact.Item> entry : this.regularRemoved.object2IntEntrySet()) {
            this.contents.amounts.addTo(entry.getKey(), entry.getIntValue());
        }
        this.exactIngredients.clear();
        this.regularRemoved.clear();
    }

    public boolean accountStack(final ItemStack stack, final int count) {
        if (this.exactIngredients.contains(stack)) {
            this.contents.account(new ItemOrExact.Exact(stack), count);
            return true;
        }
        return false;
    }
}
