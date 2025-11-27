package com.mohistmc.youer.feature.menu;

import java.util.List;
import lombok.Data;

@Data
public class MenuSettings {
    private String name;
    private int rows;
    private Integer autoRefresh;
    private List<String> openActions;
}