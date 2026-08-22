package com.mohistmc.youer.bukkit.neoforge.handler;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.bukkit.neoforge.bridge.NetBridge;
import com.mohistmc.youer.bukkit.neoforge.channel.ChannelContext;
import com.mohistmc.youer.bukkit.neoforge.payload.ChannelPayloadImpl;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public record BukkitDispatchHandler(ChannelContext<BukkitDispatchHandler> context) implements NeoPayloadReceiver {

    @Override
    public void handle(ChannelPayloadImpl pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var bukkit = ((ServerPlayer) ctx.player()).getBukkitEntity();
            context.deliver(bukkit, pkt.drain());
        });
    }

    @Override
    public void synchronize() {
        NetBridge.reflect(context);
    }

    @Override
    public void send(Plugin src, CraftPlayer dst, byte[] data) {
        if (YouerConfig.pluginchannel_debug)
            System.out.printf("sendCustomPayload: %s %s%n", context.location().toString(), new String(data, StandardCharsets.UTF_8));
        PacketDistributor.sendToPlayer(dst.getHandle(), new ChannelPayloadImpl(context.payloadType(), data));
    }
}