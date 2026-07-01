package com.mohistmc.youer.feature.item;

import com.mohistmc.youer.feature.db.ItemDatabaseStorage;
import java.util.List;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2023/8/2 18:27:05
 *
 * Item configuration storage backed by database (SQLite or MySQL).
 * Uses {@link ItemDatabaseStorage} for persistence with Zstd compression on SQLite.
 */
public class ItemsConfig {

    public static ItemsConfig INSTANCE;
    private final ItemDatabaseStorage storage;

    public ItemsConfig() {
        this.storage = new ItemDatabaseStorage();
    }

    public static void init() {
        INSTANCE = new ItemsConfig();
        INSTANCE.storage.init();
    }

    /**
     * @return all saved ItemStacks
     */
    public List<ItemStack> getItems() {
        return storage.getAllItems();
    }

    /**
     * @return all saved item keys (names)
     */
    public List<String> getItemStrings() {
        return storage.getAllKeys();
    }

    /**
     * Get an ItemStack by name.
     *
     * @param itemName the item key
     * @return the ItemStack, or AIR if not found
     */
    public ItemStack get(String itemName) {
        return storage.get(itemName);
    }

    /**
     * Remove a saved item by name.
     */
    public void remove(String itemName) {
        storage.remove(itemName);
    }

    /**
     * Save an ItemStack with a key.
     * Supports keys with or without "items." prefix for backward compatibility.
     *
     * @param key   the item key (e.g. "items.my_sword" or "my_sword")
     * @param value the ItemStack to save
     */
    public void put(String key, Object value) {
        String cleanKey = key.startsWith("items.") ? key.substring("items.".length()) : key;
        if (value instanceof ItemStack itemStack) {
            storage.put(cleanKey, itemStack);
        }
    }
}
