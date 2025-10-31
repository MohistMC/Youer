package com.mohistmc.youer.bukkit.messaging;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public interface PluginsPayload extends CustomPacketPayload {

    static <B extends FriendlyByteBuf> StreamCodec<B, PluginsDiscardedPayload> codec(Type<PluginsDiscardedPayload> type, int max) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    var size = buf.readableBytes();
                    Preconditions.checkArgument(size <= max, "Custom payload size may not be larger than " + max);
                    return buf.readRetainedSlice(size);
                }),
                PluginsPayload::getSlicedData,
                it -> new PluginsDiscardedPayload(type, it)
        );
    }

    static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> dcodec(ResourceLocation location, int max) {
        return new StreamCodec<>() {
            @Override
            public DiscardedPayload decode(B buf) {
                int j = buf.readableBytes();
                if (j >= 0 && j <= max) {
                    var data = buf.readRetainedSlice(j);
                    return new DiscardedPayload(location, data);
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than " + max + " bytes");
                }
            }

            @Override
            public void encode(B buf, CustomPacketPayload obj) {
                if (obj instanceof PluginsPayload raw) {
                    buf.writeBytes(raw.getSlicedData());
                }
            }
        };
    }

    ByteBuf getData();

    void setData(ByteBuf data);

    default byte[] readBytes() {
        final var buf = getData();
        byte[] allocate = new byte[buf.readableBytes()];
        buf.readBytes(allocate);
        ReferenceCountUtil.release(buf);
        setData(null);
        return allocate;
    }

    default ByteBuf getSlicedData() {
        return getData().slice();
    }

    default byte[] leak() {
        final var buf = getData();
        byte[] allocate = new byte[buf.readableBytes()];
        buf.readBytes(allocate);
        ReferenceCountUtil.release(buf);
        setData(null);
        return allocate;
    }
}
