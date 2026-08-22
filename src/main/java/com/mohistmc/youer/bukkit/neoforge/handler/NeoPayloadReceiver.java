package com.mohistmc.youer.bukkit.neoforge.handler;

import com.mohistmc.youer.bukkit.neoforge.payload.ChannelPayloadImpl;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public interface NeoPayloadReceiver extends ChannelWorker, IPayloadHandler<ChannelPayloadImpl> {
}