package com.mohistmc.youer.feature.menu;

import java.util.List;

public class MenuSettings {
    private String name;
    private int rows;
    private String openWithItem;
    private List<String> openActions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public String getOpenWithItem() {
        return openWithItem;
    }

    public void setOpenWithItem(String openWithItem) {
        this.openWithItem = openWithItem;
    }

    public List<String> getOpenActions() {
        return openActions;
    }

    public void setOpenActions(List<String> openActions) {
        this.openActions = openActions;
    }
}
