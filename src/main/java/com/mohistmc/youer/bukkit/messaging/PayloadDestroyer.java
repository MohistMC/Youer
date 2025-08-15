package com.mohistmc.youer.bukkit.messaging;

import com.mohistmc.youer.Youer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PayloadDestroyer extends PluginChannelHandler {

    default void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        Youer.LOGGER.debug("Ignoring sendCustomPayload for channel {} due to conflict with mod channel.", channel().getChannel());
    }

    @Override
    default void updateChannel() {
    }
}
