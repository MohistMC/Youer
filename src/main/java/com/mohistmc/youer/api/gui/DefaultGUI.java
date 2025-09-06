package com.mohistmc.youer.api.gui;

import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Player;

public class DefaultGUI {

    GUI gui;
    int pageChoose = 0;

    public DefaultGUI(GUIType type, String name) {
        this.gui = new GUI(type, name);
    }

    public final GUIItem getItem(int index) {
        return this.gui.getItem(index);
    }

    public final void setItem(int slot, GUIItem item) {
        this.gui.setItem(slot, item);
    }

    public final List<GUIItem> getItems() {
        return Arrays.stream(this.gui.items).toList();
    }

    public GUI getGUI() {
        return this.gui;
    }

    public void openGUI(Player p) {
        getGUI().openGUI(p);
    }
}

