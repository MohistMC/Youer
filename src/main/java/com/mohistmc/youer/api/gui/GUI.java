package com.mohistmc.youer.api.gui;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author LSeng
 */
public class GUI {

    public GUIItem[] items;
    public Inventory inv;
    GUIType type;
    String tempName;

    private GUI() {
    }

    public GUI(GUIType type, String name) {
        this.type = type;
        this.tempName = name;
        this.items = new GUIItem[9 * type.getRows()];
    }

    public final void setItem(GUIItem item, int... index) {
        for (int i : index) {
            setItem(i, item);
        }
    }

    public final void setItem(int index, GUIItem item) {
        this.items[index] = Objects.requireNonNullElseGet(item, () -> new GUIItem(new ItemStack(Material.AIR)));
    }

    public final GUIItem getItem(int index) {
        return this.items[index];
    }


    public void openGUI(Player p) {
        Inventory inv;
        if (this.type == GUIType.CANCEL) {
            throw new NullPointerException("Canceled or non-existent GUI");
        }
        inv = Bukkit.createInventory(null, this.items.length, this.tempName);
        for (int index = 0; index < this.items.length; index++) {
            if (items[index] == null) {
                inv.setItem(index, new ItemStack(Material.AIR));
            } else {
                inv.setItem(index, items[index].display);
            }
        }
        this.inv = inv;
        p.openInventory(inv);
        GuiListener.openGUI.put(p, this);
    }

}
