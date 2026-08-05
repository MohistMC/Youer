package com.mohistmc.youer.feature;

import com.mohistmc.tools.ListUtils;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * @author Mgazul
 * @date 2026/8/6
 */
public class EntityClearListener {

    public static EntityClearInventory openInventory;

    public static void save(InventoryCloseEvent event) {
        if (openInventory == null || openInventory.getInventory() != event.getInventory()) {
            return;
        }
        try {
            if (openInventory.getType() == EntityClearType.ITEM) {
                List<String> old = new ArrayList<>(YouerConfig.clear_item_whitelist);
                for (org.bukkit.inventory.ItemStack itemStack : event.getInventory().getContents()) {
                    if (itemStack != null && !itemStack.isEmpty()) {
                        ListUtils.isDuplicate(old, itemStack.getType().getKey().asString());
                    }
                }
                EntityClear.saveItemWhitelist(old);
                event.getPlayer().sendMessage(I18n.as("entityclear.add.item.success"));
            } else {
                List<String> old = new ArrayList<>(YouerConfig.clear_monster_whitelist);
                for (org.bukkit.inventory.ItemStack itemStack : event.getInventory().getContents()) {
                    if (itemStack != null && !itemStack.isEmpty()) {
                        ItemStack nmsItem = ItemAPI.toNMSItem(itemStack);
                        if (nmsItem.getItem() instanceof SpawnEggItem spawnEggItem) {
                            EntityType<?> entitytype = spawnEggItem.getType(nmsItem);
                            var key = BuiltInRegistries.ENTITY_TYPE.getKey(entitytype);
                            ListUtils.isDuplicate(old, key.toString());
                        }
                    }
                }
                EntityClear.saveMonsterWhitelist(old);
                event.getPlayer().sendMessage(I18n.as("entityclear.add.entity.success"));
            }
        } catch (Exception e) {
            Youer.LOGGER.warn("Failed to save entity clear whitelist", e);
        } finally {
            openInventory = null;
        }
    }
}
