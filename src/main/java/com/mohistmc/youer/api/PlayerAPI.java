package com.mohistmc.youer.api;

import com.mohistmc.youer.util.I18n;
import java.net.SocketAddress;
import java.util.LinkedHashSet;
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

    public static Map<SocketAddress, LinkedHashSet<String>> modlist = new ConcurrentHashMap<>();

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
        return !modlist.containsKey(socketAddress) ? 0 : modlist.get(socketAddress).size() - 1;
    }

    public static LinkedHashSet<String> getModlist(Player player) {
        SocketAddress socketAddress = getRemoteAddress(player);
        return modlist.getOrDefault(socketAddress, new LinkedHashSet<>());
    }

    /**
     * Add mod IDs to the player's mod list
     *
     * @param player object
     * @param modId The mod ID to be added
     */
    public static void addMod(SocketAddress player, String modId) {
        modlist.computeIfAbsent(player, k -> new LinkedHashSet<>()).add(modId);
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
        String message = des + info;
        if (des.contains("Base64")) {
            message = des + info.substring(0, 20) + "......";
        }
        TextComponent textComponent = new TextComponent(message);
        textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder("§c%s".formatted(I18n.as("itemscmd.copy"))).create())));
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, info));
        player.spigot().sendMessage(textComponent);
    }
}
