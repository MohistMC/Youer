package com.mohistmc.youer.bukkit.neoforge.channel;

import net.minecraft.network.protocol.PacketFlow;

public enum TransferDirection {
    NONE(null, (byte) 0),
    UPSTREAM(PacketFlow.SERVERBOUND, (byte) 1),
    DOWNSTREAM(PacketFlow.CLIENTBOUND, (byte) 2),
    BIDIRECTIONAL(null, (byte) 3),
    ;

    public final PacketFlow flow;
    public final byte bitmap;

    TransferDirection(PacketFlow flow, byte bitmap) {
        this.flow = flow;
        this.bitmap = bitmap;
    }
}