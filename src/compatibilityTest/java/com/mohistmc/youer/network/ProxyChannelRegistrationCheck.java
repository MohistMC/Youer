package com.mohistmc.youer.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.papermc.paper.configuration.GlobalConfiguration;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkChannel;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Replays configuration transitions against the real client initialization and send checks, without a game/server. */
public final class ProxyChannelRegistrationCheck {
    private static final FixturePayload EDIT = payload("mtr:packet_update_depot");
    private static final FixturePayload DRIVE = payload("mtr:packet_drive_train");
    private static final FixturePayload CLIENT_ONLY = payload("regression:client_only");
    private static final FixturePayload REQUIRED = payload("regression:required");
    private static final FixturePayload CONFIG_ONLY = payload("regression:configuration_only");
    private static final FixturePayload BIDIRECTIONAL = payload("regression:bidirectional");
    private static int checks;

    public static void main(String[] args) throws ReflectiveOperationException {
        // Connection reads Paper's join limit at class initialization, even without a server.
        var config = new GlobalConfiguration();
        config.misc = config.new Misc();
        config.packetLimiter = config.new PacketLimiter();
        var configInstance = GlobalConfiguration.class.getDeclaredField("instance");
        configInstance.setAccessible(true);
        configInstance.set(null, config);

        register(EDIT, ConnectionProtocol.PLAY, Optional.of(PacketFlow.SERVERBOUND), true);
        register(DRIVE, ConnectionProtocol.PLAY, Optional.of(PacketFlow.SERVERBOUND), true);
        register(CLIENT_ONLY, ConnectionProtocol.PLAY, Optional.of(PacketFlow.CLIENTBOUND), true);
        register(REQUIRED, ConnectionProtocol.PLAY, Optional.of(PacketFlow.SERVERBOUND), false);
        register(CONFIG_ONLY, ConnectionProtocol.CONFIGURATION, Optional.of(PacketFlow.SERVERBOUND), true);
        register(BIDIRECTIONAL, ConnectionProtocol.PLAY, Optional.empty(), true);

        checkConnection(false, ConnectionType.NEOFORGE);
        checkConnection(false, ConnectionType.OTHER);
        checkConnection(true, ConnectionType.NEOFORGE);
        System.out.println("Proxy channel checks passed: " + checks);
    }

    private static void checkConnection(boolean switchedBackend, ConnectionType type) {
        var client = new Connection(PacketFlow.CLIENTBOUND);
        var server = new Connection(PacketFlow.SERVERBOUND);
        var clientChannel = new EmbeddedChannel();
        var serverChannel = new EmbeddedChannel();
        client.channel = clientChannel;
        server.channel = serverChannel;
        try {
            var negotiated = new NetworkPayloadSetup(Map.of(ConnectionProtocol.PLAY, Map.of(
                    EDIT.type().id(), new NetworkChannel(EDIT.type().id(), "1"),
                    DRIVE.type().id(), new NetworkChannel(DRIVE.type().id(), "1"),
                    BIDIRECTIONAL.type().id(), new NetworkChannel(BIDIRECTIONAL.type().id(), "1"))));
            var setup = type.isNeoForge() ? negotiated : NetworkPayloadSetup.empty();
            var clientListener = listener(ClientConfigurationPacketListener.class, client, type,
                    ConnectionProtocol.CONFIGURATION, ignored -> {});
            if (type.isNeoForge()) {
                if (switchedBackend) {
                    // The proxy keeps the client socket. Its first backend has no MTR channels.
                    var lobbyListener = listener(ClientConfigurationPacketListener.class, client, type,
                            ConnectionProtocol.CONFIGURATION, ignored -> {});
                    ClientNetworkRegistry.initializeNeoForgeConnection(lobbyListener, NetworkPayloadSetup.empty());
                }
                ClientNetworkRegistry.initializeNeoForgeConnection(clientListener, setup);
                require(ClientNetworkRegistry.isConnectionInitialized(client), "Real client initialization did not run");
                if (switchedBackend) {
                    // NeoForge 26.2.0.88 initializes once per socket, not once per backend.
                    require(!NetworkRegistry.hasChannel(client, ConnectionProtocol.PLAY, EDIT.type().id()),
                            "Client now renegotiates on switching; review the compatibility scenario");
                }
            } else {
                ChannelAttributes.setPayloadSetup(client, setup);
                ChannelAttributes.setConnectionType(client, type);
            }
            ChannelAttributes.setPayloadSetup(server, setup);
            ChannelAttributes.setConnectionType(server, type);
            var received = new ArrayList<CustomPacketPayload>();
            var serverListener = listener(ServerConfigurationPacketListener.class, server, type,
                    ConnectionProtocol.CONFIGURATION, packet -> {
                        received.add(packet);
                        forward(client, packet);
                    });
            NetworkRegistry.onConfigurationFinished(serverListener);

            var clientPlay = listener(ClientCommonPacketListener.class, client, type,
                    ConnectionProtocol.PLAY, ignored -> {});
            // This is the exact guard that rejects both depot editing and boarding in the user logs.
            NetworkRegistry.checkPacket(new ServerboundCustomPayloadPacket(EDIT), clientPlay);
            checks++;
            NetworkRegistry.checkPacket(new ServerboundCustomPayloadPacket(DRIVE), clientPlay);
            checks++;
            NetworkRegistry.checkPacket(new ServerboundCustomPayloadPacket(BIDIRECTIONAL), clientPlay);
            checks++;

            var advertised = received.stream().filter(MinecraftRegisterPayload.class::isInstance)
                    .map(MinecraftRegisterPayload.class::cast).findFirst().orElseThrow().newChannels();
            require(advertised.contains(MinecraftRegisterPayload.ID), "Register channel was lost");
            require(advertised.contains(MinecraftUnregisterPayload.ID), "Unregister channel was lost");
            require(advertised.contains(ModdedNetworkQueryPayload.ID) == type.isNeoForge(), "Query capability changed");
            require(!advertised.contains(CLIENT_ONLY.type().id()), "Advertised wrong-direction payload");
            require(!advertised.contains(REQUIRED.type().id()), "Advertised unnegotiated required payload");
            require(!advertised.contains(CONFIG_ONLY.type().id()), "Leaked configuration-only payload into play");
            rejected(clientPlay, CLIENT_ONLY);
            rejected(clientPlay, REQUIRED);
            rejected(clientPlay, CONFIG_ONLY);
            rejected(clientPlay, payload("regression:unknown"));

            if (switchedBackend || type.isOther()) {
                // Ad-hoc permission remains revocable; the repair must not weaken checkPacket.
                forward(client, new MinecraftUnregisterPayload(Set.of(EDIT.type().id(), DRIVE.type().id(), BIDIRECTIONAL.type().id())));
                rejected(clientPlay, EDIT);
                rejected(clientPlay, DRIVE);
                rejected(clientPlay, BIDIRECTIONAL);
            }
            System.out.println("Connection checks passed: " + type + (switchedBackend ? " after backend switch" : " fresh"));
        } finally {
            clientChannel.finishAndReleaseAll();
            serverChannel.finishAndReleaseAll();
        }
    }

    private static void register(FixturePayload payload, ConnectionProtocol protocol, Optional<PacketFlow> flow, boolean optional) {
        NetworkRegistry.register(payload.type(), StreamCodec.unit(payload), (packet, context) -> {},
                (packet, context) -> {}, List.of(protocol), flow, "1", optional);
    }

    private static void forward(Connection client, CustomPacketPayload packet) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            // Exercise the real channel-list wire codec, not direct set injection.
            if (packet instanceof MinecraftRegisterPayload registration) {
                MinecraftRegisterPayload.STREAM_CODEC.encode(buffer, registration);
                NetworkRegistry.onMinecraftRegister(client, MinecraftRegisterPayload.STREAM_CODEC.decode(buffer).newChannels());
            } else if (packet instanceof MinecraftUnregisterPayload unregistration) {
                MinecraftUnregisterPayload.STREAM_CODEC.encode(buffer, unregistration);
                NetworkRegistry.onMinecraftUnregister(client, MinecraftUnregisterPayload.STREAM_CODEC.decode(buffer).forgottenChannels());
            } else {
                throw new AssertionError("Unexpected packet: " + packet.type().id());
            }
            require(!buffer.isReadable(), "Channel-list codec did not consume its payload");
        } finally {
            buffer.release();
        }
    }

    private static <T extends PacketListener> T listener(Class<T> api, Connection connection, ConnectionType type,
                                                        ConnectionProtocol protocol, Consumer<CustomPacketPayload> sent) {
        return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> switch (method.getName()) {
            case "getConnection" -> connection;
            case "getConnectionType" -> type;
            case "flow" -> connection.getReceiving();
            case "protocol" -> protocol;
            case "isAcceptingMessages" -> true;
            case "send" -> {
                sent.accept((CustomPacketPayload) args[0]);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.toString());
        }));
    }

    private static void rejected(ClientCommonPacketListener listener, FixturePayload payload) {
        try {
            NetworkRegistry.checkPacket(new ServerboundCustomPayloadPacket(payload), listener);
        } catch (UnsupportedOperationException expected) {
            require(expected.getMessage().contains(payload.type().id().toString()), "Unexpected rejection: " + expected);
            return;
        }
        throw new AssertionError("Client unexpectedly allowed " + payload.type().id());
    }

    private static FixturePayload payload(String name) {
        return new FixturePayload(new CustomPacketPayload.Type<>(Identifier.parse(name)));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private record FixturePayload(CustomPacketPayload.Type<FixturePayload> type) implements CustomPacketPayload {}
}
