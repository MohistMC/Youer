package cn.mohistmc.youer.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PluginsDiscardedPayload(ResourceLocation id, io.netty.buffer.ByteBuf data) implements CustomPacketPayload { // CraftBukkit - store data

    public static <T extends FriendlyByteBuf> StreamCodec<T, PluginsDiscardedPayload> codec(ResourceLocation id, int maxBytes) {
        return CustomPacketPayload.codec((discardedpayload, packetdataserializer) -> {
            packetdataserializer.writeBytes(discardedpayload.data); // CraftBukkit - serialize
        }, (packetdataserializer) -> {
            int j = packetdataserializer.readableBytes();

            if (j >= 0 && j <= maxBytes) {
                // CraftBukkit start
                return new PluginsDiscardedPayload(id, packetdataserializer.readBytes(j));
                // CraftBukkit end
            } else {
                throw new IllegalArgumentException("Payload may not be larger than " + maxBytes + " bytes");
            }
        });
    }

    @Override
    public Type<PluginsDiscardedPayload> type() {
        return new Type<>(this.id);
    }
}
