package com.mohistmc.youer.api.gui;

import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author LSeng
 */
public class DemoGUI {

    GUI gui;
    List<GUIItem> items = new ArrayList<>();
    int pageChoose = 0;

    public DemoGUI(String name) {
        this.gui = new GUI(GUIType.SIXBYNINE, name);

        for (int i = 36; i < 46; i++) {
            this.gui.setItem(i, new GUIItem(new ItemStackFactory(Material.GLASS_PANE)
                    .hideTooltip()
                    .build()));
        }

        this.gui.setItem(new GUIItem(new ItemStackFactory(Material.GLASS_PANE)
                .hideTooltip()
                .build()), 47, 48, 49, 50);

        this.gui.setItem(46, new GUIItem(new ItemStackFactory(Material.REDSTONE)
                .setDisplayName(I18n.as("demogui.close"))
                .build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                if (!type.isShiftClick() && type.isLeftClick()) {
                    u.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                }
            }
        });
    }

    public final GUIItem getItem(int index) {
        return this.items.get(index);
    }

    public final void removeItem(int index) {
        this.items.remove(index);
    }

    public final void removeItem(GUIItem item) {
        this.items.remove(item);
    }

    public final void addItem(GUIItem item) {
        this.items.add(item);
    }

    public final void setItem(int slot, GUIItem item) {
        this.gui.setItem(slot, item);
    }

    public final void addItem(List<GUIItem> items) {
        for (GUIItem item : items) {
            addItem(item);
        }
    }

    public final void addItem(GUIItem... items) {
        for (GUIItem item : items) {
            addItem(item);
        }
    }

    public final List<GUIItem> getItems() {
        return this.items;
    }

    public GUI getGUI() {
        return getGUI(this.items);
    }


    private GUI getGUI(List<GUIItem> items) {
        int page = items.size() / 36 + 1;
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, new GUIItem(new ItemStack(Material.AIR)));
        }

        int index = 0;
        if (pageChoose == page - 1) {
            if (pageChoose == 0) {
                for (int i = 0; i < items.size() % 36; i++) {
                    gui.setItem(index, items.get(i));
                    index++;
                }
            } else {
                for (int i = this.pageChoose * 36; i < pageChoose * 36 + items.size() % 36; i++) {
                    gui.setItem(index, items.get(i));
                    index++;
                }
            }
        } else if (pageChoose == 0) {
            for (int i = 0; i < 36; i++) {
                gui.setItem(index, items.get(i));
                index++;
            }
        } else {
            for (int i = this.pageChoose * 36; i < this.pageChoose * 36 + 36; i++) {
                gui.setItem(index, items.get(i));
                index++;
            }
        }

        if (this.pageChoose == 0) {
            gui.setItem(51, new GUIItem(new ItemStackFactory(Material.ACACIA_FENCE)
                    .setDisplayName(I18n.as("demogui.homepage"))
                    .build()));
        } else {
            gui.setItem(51, new GUIItem(new ItemStackFactory(Material.ACACIA_FENCE)
                    .setDisplayName(I18n.as("demogui.ppage"))
                    .build()) {
                @Override
                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                    DemoGUI.this.pageChoose--;
                    openGUI(u);
                }
            });
        }

        gui.setItem(52, new GUIItem(new ItemStackFactory(Material.PAPER, pageChoose + 1)
                .setDisplayName(I18n.as("demogui.nowpage", (pageChoose + 1)))
                .build()));

        if (this.pageChoose < page - 1) {
            gui.setItem(53, new GUIItem(new ItemStackFactory(Material.ACACIA_FENCE)
                    .setDisplayName(I18n.as("demogui.npage"))
                    .build()) {
                @Override
                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                    DemoGUI.this.pageChoose++;
                    openGUI(u);
                }
            });
        } else {
            gui.setItem(53, new GUIItem(new ItemStackFactory(Material.ACACIA_FENCE)
                    .setDisplayName(I18n.as("demogui.lastpage"))
                    .build()));
        }

        return this.gui;
    }

    public void openGUI(Player p) {
        getGUI().openGUI(p);
    }

}

