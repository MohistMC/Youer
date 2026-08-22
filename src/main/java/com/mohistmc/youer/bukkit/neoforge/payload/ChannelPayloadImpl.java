package com.mohistmc.youer.bukkit.neoforge.payload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ChannelPayloadImpl implements ChannelPayload {

    public static final Map<Identifier, Type<ChannelPayloadImpl>> REGISTRY = new HashMap<>();
    private final Type<ChannelPayloadImpl> type;
    private ByteBuf data;

    public ChannelPayloadImpl(Type<ChannelPayloadImpl> type, ByteBuf raw) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
        this.data = raw;
    }

    @Override
    public ByteBuf buffer() {
        return Objects.requireNonNull(data, "Channel payload buffer has been consumed or is not set");
    }

    @Override
    public void setBuffer(ByteBuf data) {
        this.data = data;
    }

    public ChannelPayloadImpl(Type<ChannelPayloadImpl> type, byte[] raw) {
        this(type, Unpooled.wrappedBuffer(raw));
    }

    public static CustomPacketPayload.Type<ChannelPayloadImpl> payloadTypeFor(Identifier channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    @Override
    public Type<ChannelPayloadImpl> type() {
        return type;
    }
}