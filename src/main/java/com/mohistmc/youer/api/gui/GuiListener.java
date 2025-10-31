package com.mohistmc.youer.api.gui;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GuiListener {

    public static Map<Player, GUI> openGUI = new HashMap<>();

    public static void onInventoryClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        if (openGUI.containsKey(p)) {
            GUI gui = openGUI.get(p);
            event.setCancelled(true);
            if (event.getInventory() == gui.inv) {
                int index = event.getRawSlot();
                if (index < gui.items.length) {
                    if (gui.items[index] != null) {
                        gui.items[index].ClickAction(event.getClick(), p, gui.items[index].display);
                    }
                }
            }
        }
    }

    public static void onInventoryCloseEvent(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) {
            return;
        }
        if (openGUI.containsKey(p)) {
            GUI gui = openGUI.get(p);
            if (event.getInventory() == gui.inv) {
                openGUI.remove((Player) event.getPlayer());
            }
        }
    }
}
