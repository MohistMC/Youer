package com.mohistmc.youer.bukkit.messaging;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public class PluginsDiscardedPayload implements CustomPacketPayload {

    public static final Map<ResourceLocation, Type<PluginsDiscardedPayload>> REGISTRY = new HashMap<>();
    private final Type<PluginsDiscardedPayload> type;
    private ByteBuf data;

    public PluginsDiscardedPayload(Type<PluginsDiscardedPayload> type, ByteBuf raw) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
        this.data = raw;
    }

    public PluginsDiscardedPayload(Type<PluginsDiscardedPayload> type, byte[] raw) {
        this(type, Unpooled.copiedBuffer(raw));
    }

    public static CustomPacketPayload.Type<PluginsDiscardedPayload> getType(ResourceLocation channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    public static <B extends FriendlyByteBuf> StreamCodec<B, PluginsDiscardedPayload> codec(Type<PluginsDiscardedPayload> type, int max) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    var size = buf.readableBytes();
                    Preconditions.checkArgument(size <= max, "Custom payload size may not be larger than " + max);
                    return buf.readRetainedSlice(size);
                }),
                PluginsDiscardedPayload::getData,
                it -> new PluginsDiscardedPayload(type, it)
        );
    }

    public static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> codec(ResourceLocation location, int max) {
        return new StreamCodec<>() {
            @Override
            public DiscardedPayload decode(B buf) {
                int j = buf.readableBytes();
                if (j >= 0 && j <= max) {
                    var data = buf.readRetainedSlice(j);
                    var payload = new DiscardedPayload(location);
                    ((PluginsDiscardedPayload) (Object) payload).setData(data);
                    return payload;
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than " + max + " bytes");
                }
            }

            @Override
            public void encode(B buf, CustomPacketPayload obj) {
                if (obj instanceof PluginsDiscardedPayload raw) {
                    buf.writeBytes(raw.getData());
                }
            }
        };
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }

    public byte[] readBytes() {
        final var buf = getData();
        byte[] allocate = new byte[buf.readableBytes()];
        buf.readBytes(allocate);
        return allocate;
    }

    @Override
    public Type<PluginsDiscardedPayload> type() {
        return type;
    }
}
