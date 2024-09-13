package com.mohistmc.youer.util;

import com.mohistmc.youer.MohistConfig;
import org.spigotmc.SpigotConfig;

public class ProxyUtils {

    public static boolean is() {
        return MohistConfig.velocity_enabled || SpigotConfig.bungee;
    }
}
