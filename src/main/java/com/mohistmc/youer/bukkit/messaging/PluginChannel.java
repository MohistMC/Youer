package com.mohistmc.youer.bukkit.messaging;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;


public class PluginChannel {

    public static final List<ConnectionProtocol> PROTOCOLS = List.of(ConnectionProtocol.CONFIGURATION, ConnectionProtocol.PLAY);
    private final Messenger messenger;
    private final CustomPacketPayload.Type<PluginsDiscardedPayload> type;
    private final StreamCodec<? super FriendlyByteBuf, PluginsDiscardedPayload> streamCodec;
    private final PluginPayloadHandler handler;
    private final Set<PluginMessageListenerRegistration> incoming;
    private final Set<Plugin> outgoing;

    public PluginChannel(Messenger messenger, boolean verifyChannel, ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        this.messenger = messenger;
        this.type = PluginsDiscardedPayload.getType(channel);
        this.streamCodec = PluginsDiscardedPayload.codec(this.type, 32767);
        this.handler = new PluginPayloadHandler(this, verifyChannel);
        this.incoming = Collections.unmodifiableSet(incoming);
        this.outgoing = Collections.unmodifiableSet(outgoing);
    }

    public ChannelDirection getDirection() {
        boolean hasIncoming = !incoming.isEmpty();
        boolean hasOutgoing = !outgoing.isEmpty();
        return hasIncoming && hasOutgoing ? ChannelDirection.BIDIRECTIONAL
                : hasIncoming ? ChannelDirection.INCOMING
                : hasOutgoing ? ChannelDirection.OUTGOING
                : ChannelDirection.NONE;
    }

    public PluginPayloadHandler getChannelHandler() {
        return handler;
    }

    public Set<Plugin> getOutgoing() {
        return outgoing;
    }

    public ResourceLocation getChannel() {
        return type.id();
    }

    public CustomPacketPayload.Type<PluginsDiscardedPayload> getType() {
        return type;
    }

    public StreamCodec<? super FriendlyByteBuf, PluginsDiscardedPayload> getStreamCodec() {
        return streamCodec;
    }

    public void dispatchMessage(Player src, byte[] message) {
        if (incoming.isEmpty()) {
            return;
        }
        Set.copyOf(incoming).forEach(listener ->
                listener.getListener().onPluginMessageReceived(type.id().toString(), src, message)
        );
    }

    public void sendCustomPayload(CraftPlayer dst, byte[] data) {
        if (handler == null) return;
        handler.sendCustomPayload(dst, data);
    }
}
