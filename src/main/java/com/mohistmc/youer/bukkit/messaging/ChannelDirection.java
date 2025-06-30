package com.mohistmc.youer.bukkit.messaging;

import net.minecraft.network.protocol.PacketFlow;

public enum ChannelDirection {
    NONE(null, (byte) 0),
    INCOMING(PacketFlow.SERVERBOUND, (byte) 1),
    OUTGOING(PacketFlow.CLIENTBOUND, (byte) 2),
    BIDIRECTIONAL(null, (byte) 3),
    ;

    public final PacketFlow flow;
    public final byte bitmap;

    ChannelDirection(PacketFlow flow, byte bitmap) {
        this.flow = flow;
        this.bitmap = bitmap;
    }
}
