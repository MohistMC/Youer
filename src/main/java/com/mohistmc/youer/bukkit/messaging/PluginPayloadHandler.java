package com.mohistmc.youer.bukkit.messaging;

import com.mohistmc.youer.YouerConfig;
import java.nio.charset.StandardCharsets;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
public record PluginPayloadHandler(PluginChannel channel,
                                   boolean verifyChannel) implements IPayloadHandler<PluginsDiscardedPayload> {

    @Override
    public void handle(PluginsDiscardedPayload pkt, IPayloadContext ctx) {
        if (verifyChannel) {
            ctx.enqueueWork(() -> {
                var bukkit = ctx.player().getBukkitEntity();
                channel.dispatchMessage((Player) bukkit, pkt.readBytes());
            });
        }
    }

    public void updateChannel() {
        if (verifyChannel) {
            NeoMessaging.updateChannel(channel);
        }
    }

    public void sendCustomPayload(CraftPlayer dst, byte[] data) {
        if (verifyChannel) {
            if (YouerConfig.pluginchannel_debug) System.out.printf("sendCustomPayload: %s %s%n", channel.getChannel().toString(), new String(data, StandardCharsets.UTF_8));
            PacketDistributor.sendToPlayer(dst.getHandle(), new PluginsDiscardedPayload(channel.getType(), data));
        }
    }
}
