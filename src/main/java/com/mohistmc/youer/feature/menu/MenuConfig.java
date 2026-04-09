package com.mohistmc.youer.feature.menu;

import java.util.Map;
import java.util.Objects;

public class MenuConfig {
    private MenuSettings menuSettings;
    private Map<String, Icon> icons;

    public MenuSettings getMenuSettings() {
        return menuSettings;
    }

    public void setMenuSettings(MenuSettings menuSettings) {
        this.menuSettings = menuSettings;
    }

    public Map<String, Icon> getIcons() {
        return icons;
    }

    public void setIcons(Map<String, Icon> icons) {
        this.icons = icons;
    }

    @Override
    public String toString() {
        return "MenuConfig{" +
                "menuSettings=" + menuSettings +
                ", icons=" + icons +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuConfig that = (MenuConfig) o;
        return Objects.equals(menuSettings, that.menuSettings) &&
                Objects.equals(icons, that.icons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuSettings, icons);
    }
}
