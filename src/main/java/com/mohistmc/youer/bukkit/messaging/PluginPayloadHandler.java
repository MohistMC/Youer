package com.mohistmc.youer.bukkit.messaging;

import com.mohistmc.youer.YouerConfig;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public record PluginPayloadHandler(PluginChannel<PluginPayloadHandler> channel) implements NeoForgePayloadHandler {

    @Override
    public void handle(PluginsDiscardedPayload pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var bukkit = ((ServerPlayer) ctx.player()).getBukkitEntity();
            channel.dispatchMessage(bukkit, pkt.leak());
        });
    }

    @Override
    public void updateChannel() {
        NeoMessaging.updateChannel(channel);
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        if (YouerConfig.pluginchannel_debug)
            System.out.printf("sendCustomPayload: %s %s%n", channel.getChannel().toString(), new String(data, StandardCharsets.UTF_8));
        PacketDistributor.sendToPlayer(dst.getHandle(), new PluginsDiscardedPayload(channel.getType(), data));
    }
}
