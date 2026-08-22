package com.mohistmc.youer.bukkit.neoforge.channel;

import com.mohistmc.youer.bukkit.neoforge.handler.ChannelWorker;
import com.mohistmc.youer.bukkit.neoforge.payload.ChannelPayload;
import com.mohistmc.youer.bukkit.neoforge.payload.ChannelPayloadImpl;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

public class ChannelContext<T extends ChannelWorker> {

    public static final List<ConnectionProtocol> PROTOCOLS = List.of(ConnectionProtocol.CONFIGURATION, ConnectionProtocol.PLAY);
    private final CustomPacketPayload.Type<ChannelPayloadImpl> type;
    private final StreamCodec<? super FriendlyByteBuf, ChannelPayloadImpl> codec;
    private final T worker;
    private final Set<PluginMessageListenerRegistration> incoming;
    private final Set<Plugin> outgoing;

    public ChannelContext(Function<ChannelContext<T>, T> factory, Identifier channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        this.type = ChannelPayloadImpl.payloadTypeFor(channel);
        this.codec = ChannelPayload.standardCodec(this.type, 32767);
        this.worker = factory.apply(this);
        this.incoming = Collections.unmodifiableSet(incoming);
        this.outgoing = Collections.unmodifiableSet(outgoing);
    }

    public TransferDirection traffic() {
        if (incoming.isEmpty()) {
            if (outgoing.isEmpty()) {
                return TransferDirection.NONE;
            } else {
                return TransferDirection.DOWNSTREAM;
            }
        } else {
            if (outgoing.isEmpty()) {
                return TransferDirection.UPSTREAM;
            } else {
                return TransferDirection.BIDIRECTIONAL;
            }
        }
    }

    public T worker() {
        return worker;
    }

    public CustomPacketPayload.Type<ChannelPayloadImpl> payloadType() {
        return type;
    }

    public StreamCodec<? super FriendlyByteBuf, ChannelPayloadImpl> codec() {
        return codec;
    }

    public Set<Plugin> outgoing() {
        return outgoing;
    }

    public Identifier location() {
        return type.id();
    }

    public <B extends FriendlyByteBuf> StreamCodec<B, ChannelPayloadImpl> pointCodec() {
        // This is very OK for our implementation
        // ByteBuf is always an input argument
        return (StreamCodec) codec;
    }

    public void deliver(Player src, byte[] message) {
        var fire = Set.copyOf(this.incoming);
        if (fire.isEmpty()) {
            return;
        }
        for (var listener : fire) {
            listener.getListener().onPluginMessageReceived(type.id().toString(), src, message);
        }
    }

    public void send(Plugin src, CraftPlayer dst, byte[] data) {
        worker.send(src, dst, data);
    }
}