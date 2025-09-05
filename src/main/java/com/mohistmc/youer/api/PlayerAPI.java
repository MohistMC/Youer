package com.mohistmc.youer.api;

import com.mohistmc.youer.util.I18n;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PlayerAPI {

    public static Map<SocketAddress, Integer> mods = new ConcurrentHashMap<>();
    public static Map<SocketAddress, List<String>> modlist = new ConcurrentHashMap<>();

    /**
     * Get Player ping
     *
     * @param player org.bukkit.entity.player
     */
    public static String getPing(Player player) {
        return String.valueOf(getNMSPlayer(player).connection.latency());
    }

    public static ServerPlayer getNMSPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public static Player getCBPlayer(ServerPlayer player) {
        return player.getBukkitEntity().getPlayer();
    }

    // Don't count the default number of mods
    public static int getModSize(Player player) {
        SocketAddress socketAddress = getRemoteAddress(player);
        return mods.get(socketAddress) == null ? 0 : mods.get(socketAddress) - 2;
    }

    public static List<String> getModlist(Player player) {
        SocketAddress socketAddress = getRemoteAddress(player);
        return modlist.get(socketAddress) == null ? Collections.emptyList() : modlist.get(socketAddress);
    }

    public static boolean hasMod(Player player, String modid) {
        return getModlist(player).contains(modid);
    }

    public static boolean isOp(ServerPlayer ep) {
        return MinecraftServer.getServer().getPlayerList().isOp(ep.getGameProfile());
    }

    public static SocketAddress getRemoteAddress(Player player) {
        return getNMSPlayer(player).connection.connection.getRemoteAddress();
    }

    public static void sendMessageByCopy(Player player, String des, String info) {
        TextComponent textComponent = new TextComponent(des + info);
        textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder("§c%s".formatted(I18n.as("itemscmd.copy"))).create())));
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, info));
        player.spigot().sendMessage(textComponent);
    }
}
