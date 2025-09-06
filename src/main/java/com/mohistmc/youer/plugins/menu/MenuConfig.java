package com.mohistmc.youer.plugins.menu;

import java.util.Map;
import lombok.Data;

@Data
public class MenuConfig {
    private MenuSettings menuSettings;
    private Map<String, Icon> icons;
}