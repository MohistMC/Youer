package com.mohistmc.youer.bukkit.messaging;

import com.mohistmc.youer.Youer;
import com.mohistmc.youer.util.I18n;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

public class NeoMessaging {

    public static String CHANNEL_VERSION = "youer:neomessaging";

    public static PluginChannel setupChannel(ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        if (verifyChannel(location, incoming, outgoing)) {
            return new PluginChannel<>(PluginPayloadHandler::new, location, incoming, outgoing);
        } else {
            return new PluginChannel<>(NeoForgePayloadDestroyer::new, location, incoming, outgoing);
        }
    }

    public static boolean verifyChannel(ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        for (var protocol : PluginChannel.PROTOCOLS) {
            var known = NetworkRegistry.PAYLOAD_REGISTRATIONS.get(protocol).get(location);
            var builtin = NetworkRegistry.BUILTIN_PAYLOADS.get(location);
            if (known != null || builtin != null) {
                var pluginList = Stream.concat(outgoing.stream(), incoming.stream().map(PluginMessageListenerRegistration::getPlugin))
                        .distinct()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                Youer.LOGGER.error(I18n.as("neoforge.channel_conflict"));
                Youer.LOGGER.error(I18n.as("neoforge.channel_details"), location, protocol);
                Youer.LOGGER.error(I18n.as("neoforge.plugin_registration"), pluginList);
                if (known != null) {
                    Youer.LOGGER.error(I18n.as("neoforge.mod_version"), known.version());
                }
                Youer.LOGGER.error(I18n.as("neoforge.channel_ignored"));
                return false;
            }
        }
        return true;
    }

    public static void updateChannel(PluginChannel channel) {
        final var location = channel.getChannel();
        for (var protocol : PluginChannel.PROTOCOLS) {
            var map = NetworkRegistry.PAYLOAD_REGISTRATIONS.get(protocol);
            if (channel.getDirection() != getFlowFromRegistration(map.get(location))) {
                final var registration = createRegistration(channel);
                if (registration == null) {
                    map.remove(location);
                } else {
                    map.put(location, registration);
                }
            }
        }
    }

    private static ChannelDirection getFlowFromRegistration(PayloadRegistration<?> registration) {
        return registration == null ? ChannelDirection.NONE :
                registration.flow().map(flow ->
                        flow == PacketFlow.SERVERBOUND ? ChannelDirection.INCOMING : ChannelDirection.OUTGOING
                ).orElse(ChannelDirection.BIDIRECTIONAL);
    }

    public static PayloadRegistration<?> createRegistration(PluginChannel<NeoForgePayloadHandler> channel) {
        var direction = channel.getDirection();
        if (direction.bitmap == 0) {
            return null;
        }
        var handler = channel.getChannelHandler();
        var type = channel.getType();
        var codec = channel.getStreamCodec();
        var flow = channel.getDirection().flow;

        return new PayloadRegistration<>(type, codec, handler, PluginChannel.PROTOCOLS, Optional.ofNullable(flow), CHANNEL_VERSION, true);
    }
}
