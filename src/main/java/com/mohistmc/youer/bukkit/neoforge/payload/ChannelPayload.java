package com.mohistmc.youer.bukkit.neoforge.payload;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;

public interface ChannelPayload extends CustomPacketPayload {

    static <B extends FriendlyByteBuf> StreamCodec<B, ChannelPayloadImpl> standardCodec(Type<ChannelPayloadImpl> type, int max) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    var size = buf.readableBytes();
                    Preconditions.checkArgument(size <= max, "Custom payload size may not be larger than " + max);
                    return buf.readRetainedSlice(size);
                }),
                ChannelPayload::slice,
                it -> new ChannelPayloadImpl(type, it)
        );
    }

    static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> deserializer(Identifier location, int max) {
        return new StreamCodec<>() {
            @Override
            public DiscardedPayload decode(B buf) {
                int j = buf.readableBytes();
                if (j >= 0 && j <= max) {
                    final byte[] data = new byte[j];
                    buf.readBytes(data);
                    return new DiscardedPayload(location, data);
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than " + max + " bytes");
                }
            }

            @Override
            public void encode(B buf, CustomPacketPayload obj) {
                if (obj instanceof ChannelPayload raw) {
                    buf.writeBytes(raw.slice());
                }
            }
        };
    }

    ByteBuf buffer();

    void setBuffer(ByteBuf data);

    default byte[] consume() {
        final var buf = buffer();
        byte[] allocate = new byte[buf.readableBytes()];
        buf.readBytes(allocate);
        ReferenceCountUtil.release(buf);
        setBuffer(null);
        return allocate;
    }

    default ByteBuf slice() {
        return buffer().slice();
    }

    default byte[] drain() {
        final var buf = buffer();
        byte[] allocate = new byte[buf.readableBytes()];
        buf.readBytes(allocate);
        ReferenceCountUtil.release(buf);
        setBuffer(null);
        return allocate;
    }
}