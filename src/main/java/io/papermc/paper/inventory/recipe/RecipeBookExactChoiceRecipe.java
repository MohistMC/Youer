package io.papermc.paper.inventory.recipe;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public abstract class RecipeBookExactChoiceRecipe<C extends net.minecraft.world.item.crafting.RecipeInput> implements Recipe<C> {

    private boolean hasExactIngredients;

    protected final void checkExactIngredients() {
        try {
            // skip any special recipes
            if (this.isSpecial()) {
                this.hasExactIngredients = false;
                return;
            }

            for (final Ingredient ingredient : this.getIngredients()) {
                if (!ingredient.isEmpty() && ingredient.exact) {
                    this.hasExactIngredients = true;
                    return;
                }
            }
        } catch (NullPointerException e) {
            // Handle case where 'isSpecial()' throws NPE due to uninitialized internal state
            this.hasExactIngredients = false;
            return;
        }
        this.hasExactIngredients = false;
    }

    @Override
    public final boolean hasExactIngredients() {
        return this.hasExactIngredients;
    }
}
