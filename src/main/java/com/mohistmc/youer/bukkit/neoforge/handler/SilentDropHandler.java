package com.mohistmc.youer.bukkit.neoforge.handler;

import com.mohistmc.youer.Youer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface SilentDropHandler extends ChannelWorker {

    default void send(Plugin src, CraftPlayer dst, byte[] data) {
        Youer.LOGGER.debug("Ignoring sendCustomPayload for channel {} due to conflict with mod channel.", context().location());
    }

    @Override
    default void synchronize() {
    }
}