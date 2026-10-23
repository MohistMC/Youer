package com.mohistmc.youer.feature.menu;

import java.util.Map;

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
}
