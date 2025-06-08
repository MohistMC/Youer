package cn.mohistmc.youer.util;

import cn.mohistmc.youer.YouerConfig;
import io.papermc.paper.configuration.GlobalConfiguration;
import org.spigotmc.SpigotConfig;

public class ProxyUtils {

    public static boolean is() {
        return GlobalConfiguration.get().proxies.velocity.enabled || SpigotConfig.bungee;
    }
}
