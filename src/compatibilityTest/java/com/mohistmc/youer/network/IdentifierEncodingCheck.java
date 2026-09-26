package com.mohistmc.youer.network;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** Exercises the real wire encoders without starting a server or connecting to a proxy. */
public final class IdentifierEncodingCheck {
    public static void main(String[] args) throws Exception {
        try (var proxy = new URLClassLoader(args.length == 0 ? new java.net.URL[0]
                : new java.net.URL[]{Path.of(args[0]).toUri().toURL()})) {
            Method transform = args.length == 0 ? null : proxy
                    .loadClass("com.velocitypowered.proxy.protocol.util.PluginMessageUtil")
                    .getMethod("transformLegacyToModernChannel", String.class);
            int checks = 0;
            for (String name : new String[]{"minecraft:register", "minecraft:unregister", "minecraft:brand",
                    "mtr:packet_drive_train", "mtr:packet_update_depot", "neoforge:register", "paper:brand"}) {
                Identifier identifier = Identifier.parse(name);
                for (boolean streamCodec : new boolean[]{false, true}) {
                    var buffer = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        if (streamCodec) {
                            Identifier.STREAM_CODEC.encode(buffer, identifier);
                        } else {
                            buffer.writeIdentifier(identifier);
                        }
                        String wireName = buffer.readUtf();
                        require(name.equals(wireName), "Network encoder shortened " + name + " to " + wireName);
                        checks++;
                        buffer.readerIndex(0);
                        Identifier decoded = streamCodec ? Identifier.STREAM_CODEC.decode(buffer) : buffer.readIdentifier();
                        require(identifier.equals(decoded) && !buffer.isReadable(), "Identifier round trip failed: " + name);
                        checks++;
                        if (transform != null) {
                            Object forwarded = transform.invoke(null, wireName);
                            require(name.equals(forwarded), "Proxy changed " + name + " into " + forwarded);
                            checks++;
                        }
                    } finally {
                        buffer.release();
                    }
                }
            }
            if (transform != null) {
                // Negative control: the old, shortened encoding must exercise the actual proxy bug.
                require("legacy:register".equals(transform.invoke(null, "register")),
                        "The proxy no longer reproduces the legacy-channel conversion; review this regression");
                checks++;
            }
            System.out.println("Identifier encoding checks passed: " + checks);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
