package com.mohistmc.youer.util;

import com.mohistmc.youer.api.ServerAPI;
import com.mojang.brigadier.StringReader;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

public class LambdaFix {

    public static boolean checkBelowWorld(Entity entity) {
        return entity.level().paperConfig().environment.netherCeilingVoidDamageHeight.test(v -> entity.getY() >= v);
    }

    public static void lambda$handleCustomCommandSuggestions0$2(ServerGamePacketListenerImpl packetListener, ServerboundCommandSuggestionPacket packet, StringReader stringreader) {
        ServerAPI.getNMSServer().scheduleOnMain(() -> packetListener.sendServerSuggestions(packet, stringreader));
    }

    public static void handleChat(ServerGamePacketListenerImpl packetListener, ServerboundChatPacket p_9841_, Optional<LastSeenMessages> optional) {
        packetListener.tryHandleChat(p_9841_.message(), () -> {
            PlayerChatMessage playerchatmessage;
            try {
                playerchatmessage = packetListener.getSignedMessage(p_9841_, optional.get());
            } catch (SignedMessageChain.DecodeException signedmessagechain$decodeexception) {
                packetListener.handleMessageDecodeFailure(signedmessagechain$decodeexception);
                return;
            }

            CompletableFuture<FilteredText> completablefuture = packetListener.filterTextPacket(playerchatmessage.signedContent()).thenApplyAsync(Function.identity(), packetListener.server.chatExecutor); // CraftBukkit - async chat
            CompletableFuture<Component> componentFuture = net.neoforged.neoforge.common.CommonHooks.getServerChatSubmittedDecorator().decorate(packetListener.player, playerchatmessage.decoratedContent()); // Paper - Adventure
            packetListener.chatMessageChain.append(CompletableFuture.allOf(completablefuture, componentFuture), p_300785_ -> {
                if (componentFuture.join() == null) return; // Forge: ServerChatEvent was canceled if this is null.
                PlayerChatMessage playerchatmessage1 = playerchatmessage.withUnsignedContent(componentFuture.join()).filter(completablefuture.join().mask());
                packetListener.broadcastChatMessage(playerchatmessage1);
            });
        }, false); // CraftBukkit - async chat
    }
}
