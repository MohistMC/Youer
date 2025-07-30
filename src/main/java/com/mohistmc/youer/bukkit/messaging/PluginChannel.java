package com.mohistmc.youer.bukkit.messaging;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

public class PluginChannel<T extends PluginChannelHandler> {

    public static final List<ConnectionProtocol> PROTOCOLS = List.of(ConnectionProtocol.CONFIGURATION, ConnectionProtocol.PLAY);
    @Getter
    private final CustomPacketPayload.Type<PluginsDiscardedPayload> type;
    @Getter
    private final StreamCodec<? super FriendlyByteBuf, PluginsDiscardedPayload> streamCodec;
    private final T handler;
    private final Set<PluginMessageListenerRegistration> incoming;
    @Getter
    private final Set<Plugin> outgoing;

    public PluginChannel(Function<PluginChannel<T>, T> factory, ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        this.type = PluginsDiscardedPayload.getType(channel);
        this.streamCodec = PluginsPayload.codec(this.type, 32767);
        this.handler = factory.apply(this);
        this.incoming = Collections.unmodifiableSet(incoming);
        this.outgoing = Collections.unmodifiableSet(outgoing);
    }

    public ChannelDirection getDirection() {
        if (incoming.isEmpty()) {
            if (outgoing.isEmpty()) {
                return ChannelDirection.NONE;
            } else {
                return ChannelDirection.OUTGOING;
            }
        } else {
            if (outgoing.isEmpty()) {
                return ChannelDirection.INCOMING;
            } else {
                return ChannelDirection.BIDIRECTIONAL;
            }
        }
    }

    public T getChannelHandler() {
        return handler;
    }

    public ResourceLocation getChannel() {
        return type.id();
    }

    public <B extends FriendlyByteBuf> StreamCodec<B, PluginsDiscardedPayload> getCast() {
        // This is very OK for our implementation
        // ByteBuf is always an input argument
        return (StreamCodec) streamCodec;
    }

    public void dispatchMessage(Player src, byte[] message) {
        var fire = Set.copyOf(this.incoming);
        if (fire.isEmpty()) {
            return;
        }
        for (var listener : fire) {
            listener.getListener().onPluginMessageReceived(type.id().toString(), src, message);
        }
    }

    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        handler.sendCustomPayload(src, dst, data);
    }
}
