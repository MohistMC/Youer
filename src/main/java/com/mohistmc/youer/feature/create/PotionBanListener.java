package com.mohistmc.youer.feature.create;

import com.mohistmc.youer.Youer;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.util.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * Handles the add-GUI for /create_item_drain potionban add: recognizes the potions placed in the GUI on close.
 *
 * @author Mgazul
 * @date 2026/8/7
 */
public class PotionBanListener {

    public static PotionBanSaveInventory openInventory;

    public static void save(InventoryCloseEvent event) {
        if (openInventory == null || openInventory.getInventory() != event.getInventory()) {
            return;
        }
        try {
            Inventory inventory = event.getInventory();
            int added = 0;
            for (org.bukkit.inventory.ItemStack itemStack : inventory.getContents()) {
                if (itemStack != null && !itemStack.isEmpty()) {
                    PotionContents contents = ItemAPI.toNMSItem(itemStack).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                    if (contents.potion().isPresent() && PotionBanConfig.INSTANCE.addIfAbsent(contents.potion().get().getRegisteredName())) {
                        added++;
                    }
                }
            }
            event.getPlayer().sendMessage(I18n.as("createitemdraincmd.potionban.add.gui.done", added));
        } catch (Exception e) {
            Youer.LOGGER.warn(I18n.as("createitemdraincmd.potionban.add.gui.failed"), e);
        } finally {
            openInventory = null;
        }
    }
}
