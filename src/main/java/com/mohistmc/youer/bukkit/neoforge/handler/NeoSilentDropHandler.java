package com.mohistmc.youer.bukkit.neoforge.handler;

import com.mohistmc.youer.bukkit.neoforge.channel.ChannelContext;
import com.mohistmc.youer.bukkit.neoforge.payload.ChannelPayloadImpl;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NeoSilentDropHandler(
        ChannelContext<NeoSilentDropHandler> context) implements NeoPayloadReceiver, SilentDropHandler {
    @Override
    public void handle(ChannelPayloadImpl arg, IPayloadContext iPayloadContext) {
    }
}