package com.mohistmc.youer.neoforge;

import com.mohistmc.youer.api.ServerAPI;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ModSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public ModSpecialRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, new ItemStack(Material.AIR), null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return CraftItemStack.asCraftMirror(this.recipe.getResultItem(ServerAPI.getNMSServer().registryAccess()));
    }

    @Override
    public void addToCraftingManager() {
        ServerAPI.getNMSServer().getRecipeManager().addRecipe(new RecipeHolder<>(CraftNamespacedKey.toMinecraft(this.getKey()), this.recipe));
    }
}
