package com.mohistmc.youer.bukkit.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class PluginsDiscardedPayload implements PluginsPayload {

    public static final Map<ResourceLocation, Type<PluginsDiscardedPayload>> REGISTRY = new HashMap<>();
    private final Type<PluginsDiscardedPayload> type;
    @Setter
    @Getter
    private ByteBuf data;

    public PluginsDiscardedPayload(Type<PluginsDiscardedPayload> type, ByteBuf raw) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
        this.data = raw;
    }

    public PluginsDiscardedPayload(Type<PluginsDiscardedPayload> type, byte[] raw) {
        this(type, Unpooled.wrappedBuffer(raw));
    }

    public static CustomPacketPayload.Type<PluginsDiscardedPayload> getType(ResourceLocation channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    @Override
    public Type<PluginsDiscardedPayload> type() {
        return type;
    }
}