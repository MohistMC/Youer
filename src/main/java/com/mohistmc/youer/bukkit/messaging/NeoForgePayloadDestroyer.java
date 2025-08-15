package com.mohistmc.youer.bukkit.messaging;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NeoForgePayloadDestroyer(
        PluginChannel<NeoForgePayloadDestroyer> channel) implements NeoForgePayloadHandler, PayloadDestroyer {
    @Override
    public void handle(PluginsDiscardedPayload arg, IPayloadContext iPayloadContext) {
    }
}
