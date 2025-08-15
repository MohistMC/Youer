package com.mohistmc.youer.bukkit.messaging;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PluginChannelHandler {
    PluginChannel<?> channel();

    void updateChannel();

    void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data);
}
