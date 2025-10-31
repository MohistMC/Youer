package com.mohistmc.youer.bukkit;

import com.mohistmc.youer.util.I18n;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.configuration.GlobalConfiguration;
import java.util.concurrent.ExecutionException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;

public class LoginHandler {

    public static void disconnect(ServerLoginPacketListenerImpl serverGamePacketListener, String pTextComponent) {
        Waitable<Object> waitable = new Waitable<>() {
            @Override
            protected Object evaluate() {
                serverGamePacketListener.disconnect(Component.literal(pTextComponent));
                return null;
            }
        };

        serverGamePacketListener.server.processQueue.add(waitable);

        try {
            waitable.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void fireEvents(ServerLoginPacketListenerImpl serverLoginPacketListener, GameProfile gameprofile) throws Exception {
        // Paper start - Velocity support
        if (serverLoginPacketListener.velocityLoginMessageId == -1 && GlobalConfiguration.get().proxies.velocity.enabled) {
            serverLoginPacketListener.disconnect(I18n.as("velocity.requires"));
            return;
        }
        // Paper end
        String playerName = gameprofile.getName();
        java.net.InetAddress address = ((java.net.InetSocketAddress) serverLoginPacketListener.connection.getRemoteAddress()).getAddress();
        java.util.UUID uniqueId = gameprofile.getId();
        final CraftServer server = serverLoginPacketListener.server.server;

        AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName, address, uniqueId);
        server.getPluginManager().callEvent(asyncEvent);

        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0) {
            final PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, address, uniqueId);
            if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
                event.disallow(asyncEvent.getResult(), asyncEvent.kickMessage()); // Paper - Adventure
            }
            Waitable<Result> waitable = new Waitable<>() {
                @Override
                protected PlayerPreLoginEvent.Result evaluate() {
                    server.getPluginManager().callEvent(event);
                    return event.getResult();
                }
            };

            serverLoginPacketListener.server.processQueue.add(waitable);
            if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED) {
                serverLoginPacketListener.disconnect(io.papermc.paper.adventure.PaperAdventure.asVanilla(event.kickMessage())); // Paper - Adventure
                return;
            }
        } else {
            if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                serverLoginPacketListener.disconnect(io.papermc.paper.adventure.PaperAdventure.asVanilla(asyncEvent.kickMessage())); // Paper - Adventure
                return;
            }
        }
        ServerLoginPacketListenerImpl.LOGGER.info("UUID of player {} is {}", gameprofile.getName(), gameprofile.getId());
        // CraftBukkit end
    }
}
