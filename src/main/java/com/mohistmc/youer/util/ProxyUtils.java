package com.mohistmc.youer.util;

/**
 * @author Mgazul
 * @date 2026/7/21 16:39
 */
public class ProxyUtils {

    public static boolean useProxy() {
        return org.spigotmc.SpigotConfig.bungee || io.papermc.paper.configuration.GlobalConfiguration.get().proxies.velocity.enabled;
    }
}
