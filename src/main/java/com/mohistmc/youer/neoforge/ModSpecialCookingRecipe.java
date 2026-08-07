package com.mohistmc.youer.neoforge;

import com.mohistmc.youer.api.ServerAPI;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2026/8/8 01:10
 */
public class ModSpecialCookingRecipe extends CraftFurnaceRecipe {

    private final Recipe<?> recipe;

    public ModSpecialCookingRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, new ItemStack(Material.AIR), new RecipeChoice.MaterialChoice(Material.STONE), 0, 200);
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