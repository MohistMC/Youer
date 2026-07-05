package com.mohistmc.youer.api;

import com.mohistmc.youer.util.I18n;
import java.net.SocketAddress;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PlayerAPI {

    public static Map<SocketAddress, LinkedHashSet<String>> modlist = new ConcurrentHashMap<>();

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
        return getCBPlayer(ep).isOp();
    }

    public static SocketAddress getRemoteAddress(Player player) {
        return getNMSPlayer(player).connection.getConnection().getRemoteAddress();
    }

    public static void sendMessageByCopy(Player player, String des, String info) {
        player.sendMessage(
                ColorAPI.adventure(des)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(info))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                ColorAPI.adventure("§c" + I18n.as("itemscmd.copy"))))
        );
    }
}
