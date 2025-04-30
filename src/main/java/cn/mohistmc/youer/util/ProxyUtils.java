package cn.mohistmc.youer.util;

import cn.mohistmc.youer.YouerConfig;
import org.spigotmc.SpigotConfig;

public class ProxyUtils {

    public static boolean is() {
        return YouerConfig.velocity_enabled || SpigotConfig.bungee;
    }
}
