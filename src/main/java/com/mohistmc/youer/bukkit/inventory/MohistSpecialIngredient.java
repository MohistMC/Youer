package com.mohistmc.youer.bukkit.inventory;

import java.util.Objects;
import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/20 23:18:51
 */
public record MohistSpecialIngredient(Ingredient ingredient) implements RecipeChoice {

    @Override
    public ItemStack getItemStack() {
        net.minecraft.world.item.ItemStack[] items = ingredient.getItems();
        return items.length > 0 ? CraftItemStack.asCraftMirror(items[0]) : new ItemStack(Material.AIR, 0);
    }

    @Override
    public RecipeChoice clone() {
        try {
            return (RecipeChoice) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return ingredient.test(CraftItemStack.asNMSCopy(itemStack));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MohistSpecialIngredient that = (MohistSpecialIngredient) o;
        return Objects.equals(ingredient, that.ingredient);
    }

}
