package com.mohistmc.youer.feature.menu;

import java.util.Map;
import lombok.Data;

@Data
public class MenuConfig {
    private MenuSettings menuSettings;
    private Map<String, Icon> icons;
}