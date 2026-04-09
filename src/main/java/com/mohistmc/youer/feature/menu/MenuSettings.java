package com.mohistmc.youer.feature.menu;

import java.util.List;
import java.util.Objects;

public class MenuSettings {
    private String name;
    private int rows;
    private Integer autoRefresh;
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

    public Integer getAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(Integer autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public List<String> getOpenActions() {
        return openActions;
    }

    public void setOpenActions(List<String> openActions) {
        this.openActions = openActions;
    }

    @Override
    public String toString() {
        return "MenuSettings{" +
                "name='" + name + '\'' +
                ", rows=" + rows +
                ", autoRefresh=" + autoRefresh +
                ", openActions=" + openActions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuSettings that = (MenuSettings) o;
        return rows == that.rows &&
                Objects.equals(name, that.name) &&
                Objects.equals(autoRefresh, that.autoRefresh) &&
                Objects.equals(openActions, that.openActions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rows, autoRefresh, openActions);
    }
}
