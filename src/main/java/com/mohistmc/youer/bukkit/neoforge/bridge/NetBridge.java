package com.mohistmc.youer.bukkit.neoforge.bridge;

import com.mohistmc.youer.Youer;
import com.mohistmc.youer.bukkit.neoforge.channel.ChannelContext;
import com.mohistmc.youer.bukkit.neoforge.channel.TransferDirection;
import com.mohistmc.youer.bukkit.neoforge.handler.BukkitDispatchHandler;
import com.mohistmc.youer.bukkit.neoforge.handler.NeoPayloadReceiver;
import com.mohistmc.youer.bukkit.neoforge.handler.NeoSilentDropHandler;
import com.mohistmc.youer.util.I18n;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

public class NetBridge {

    public static String CHANNEL_VERSION = "youer:neomessaging";

    public static boolean versionsMatch(String left, String right) {
        if (left.equals(CHANNEL_VERSION) || right.equals(CHANNEL_VERSION)) {
            return true;
        }
        return left.equals(right);
    }

    public static ChannelContext open(Identifier location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        if (isAvailable(location, incoming, outgoing)) {
            return new ChannelContext<>(BukkitDispatchHandler::new, location, incoming, outgoing);
        } else {
            return new ChannelContext<>(NeoSilentDropHandler::new, location, incoming, outgoing);
        }
    }

    public static boolean isAvailable(Identifier location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        for (var protocol : ChannelContext.PROTOCOLS) {
            var known = NetworkRegistry.PAYLOAD_REGISTRATIONS.get(protocol).get(location);
            var builtin = NetworkRegistry.isBuiltinPayload(location);
            if (known != null || builtin) {
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

    public static void reflect(ChannelContext channel) {
        final var location = channel.location();
        for (var protocol : ChannelContext.PROTOCOLS) {
            var map = NetworkRegistry.PAYLOAD_REGISTRATIONS.get(protocol);
            if (channel.traffic() != transferFlowOf(map.get(location))) {
                // Drop any prior dynamic registration first so a direction change (e.g. UPSTREAM -> BIDIRECTIONAL)
                // does not clash with already registered handlers.
                NetworkRegistry.unregisterDynamicPayload(protocol, location);
                if (!reveal(protocol, (ChannelContext<NeoPayloadReceiver>) channel)) {
                    NetworkRegistry.unregisterDynamicPayload(protocol, location);
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean reveal(ConnectionProtocol protocol, ChannelContext<NeoPayloadReceiver> channel) {
        var direction = channel.traffic();
        if (direction.bitmap == 0) {
            return false;
        }
        var handler = channel.worker();
        var type = channel.payloadType();
        var codec = channel.codec();
        var flow = direction.flow;

        IPayloadHandler serverHandler = null;
        IPayloadHandler clientHandler = null;
        if (direction == TransferDirection.UPSTREAM) {
            serverHandler = handler;
        } else if (direction == TransferDirection.DOWNSTREAM) {
            clientHandler = handler;
        } else {
            serverHandler = handler;
            clientHandler = handler;
        }

        NetworkRegistry.registerDynamicPayload(type, codec, serverHandler, clientHandler, List.of(protocol), Optional.ofNullable(flow), CHANNEL_VERSION, true);
        return true;
    }

    private static TransferDirection transferFlowOf(PayloadRegistration<?> registration) {
        return registration == null ? TransferDirection.NONE :
                registration.flow().map(flow ->
                        flow == PacketFlow.SERVERBOUND ? TransferDirection.UPSTREAM : TransferDirection.DOWNSTREAM
                ).orElse(TransferDirection.BIDIRECTIONAL);
    }
}