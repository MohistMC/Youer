package io.papermc.paper.inventory.recipe;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public final class StackedContentsExtraMap {

    private final AtomicInteger idCounter = new AtomicInteger(BuiltInRegistries.ITEM.size()); // start at max vanilla stacked contents idx
    public final Object2IntMap<ItemStack> exactChoiceIds = new Object2IntOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
    private final Int2ObjectMap<ItemStack> idToExactChoice = new Int2ObjectOpenHashMap<>();
    private final StackedContents contents;
    public final Map<Ingredient, IntList> extraStackingIds = new IdentityHashMap<>();

    public StackedContentsExtraMap(final StackedContents contents, final Recipe<?> recipe) {
        this.exactChoiceIds.defaultReturnValue(-1);
        this.contents = contents;
        this.initialize(recipe);
    }

    private void initialize(final Recipe<?> recipe) {
        if (recipe.hasExactIngredients()) {
            for (final Ingredient ingredient : recipe.getIngredients()) {
                if (!ingredient.isEmpty() && ingredient.exact) {
                    final ItemStack[] items = ingredient.getItems();
                    final IntList idList = new IntArrayList(items.length);
                    for (final ItemStack item : items) {
                        idList.add(this.registerExact(item)); // I think not copying the stack here is safe because cb copies the stack when creating the ingredient
                        if (item.getComponentsPatch().isEmpty()) {
                            // add regular index if it's a plain itemstack but still registered as exact
                            idList.add(StackedContents.getStackingIndex(item));
                        }
                    }
                    idList.sort(IntComparators.NATURAL_COMPARATOR);
                    this.extraStackingIds.put(ingredient, idList);
                }
            }
        }
    }

    private int registerExact(final ItemStack exactChoice) {
        final int existing = this.exactChoiceIds.getInt(exactChoice);
        if (existing > -1) {
            return existing;
        }
        final int id = this.idCounter.getAndIncrement();
        this.exactChoiceIds.put(exactChoice, id);
        this.idToExactChoice.put(id, exactChoice);
        return id;
    }

    public ItemStack getById(int id) {
        return this.idToExactChoice.get(id);
    }

    public Int2IntMap regularRemoved = new Int2IntArrayMap();
    public void accountInput(final CraftingInput input) {
        // similar logic to the CraftingInput constructor
        for (final ItemStack item : input.items()) {
            if (!item.isEmpty()) {
                if (this.accountStack(item, 1)) {
                    // remove one of the items if it was added to the contents as a non-extra item
                    final int plainStackIdx = StackedContents.getStackingIndex(item);
                    if (this.contents.take(plainStackIdx, 1) == plainStackIdx) {
                        this.regularRemoved.put(plainStackIdx, 1);
                    }
                }
            }
        }
    }

    public void resetExtras() {
        // clear previous extra ids
        for (final int extraId : this.exactChoiceIds.values()) {
            this.contents.contents.remove(extraId);
        }
        for (final Int2IntMap.Entry entry : this.regularRemoved.int2IntEntrySet()) {
            this.contents.put(entry.getIntKey(), entry.getIntValue());
        }
    }

    public boolean accountStack(final ItemStack stack, final int count) {
        if (!this.exactChoiceIds.isEmpty()) {
            final int id = this.exactChoiceIds.getInt(stack);
            if (id >= 0) {
                this.contents.put(id, count);
                return true;
            }
        }
        return false;
    }
}
