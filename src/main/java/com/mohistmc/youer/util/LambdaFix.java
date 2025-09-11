package com.mohistmc.youer.util;

import com.mohistmc.youer.api.ServerAPI;
import com.mojang.brigadier.StringReader;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

public class LambdaFix {

    public static boolean checkBelowWorld(Entity entity) {
        return entity.level().paperConfig().environment.netherCeilingVoidDamageHeight.test(v -> entity.getY() >= v);
    }

    public static void lambda$handleCustomCommandSuggestions0$2(ServerGamePacketListenerImpl packetListener, ServerboundCommandSuggestionPacket packet, StringReader stringreader) {
        ServerAPI.getNMSServer().scheduleOnMain(() -> packetListener.sendServerSuggestions(packet, stringreader));
    }
}
