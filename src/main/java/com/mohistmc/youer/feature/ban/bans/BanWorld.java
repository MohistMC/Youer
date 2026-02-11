package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.tools.ListUtils;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import com.mohistmc.youer.feature.ban.ClickType;
import com.mohistmc.youer.feature.ban.utils.BanUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Player;

/**
 * @author Mgazul
 * @date 2026/2/10 22:32
 */
public class BanWorld {

    public static Set<ResourceLocation> CACHE = new HashSet<>();

    public static void addBan(Player player, String key) {
        List<String> old = BanConfig.getListByType(BanType.WORLD);
        ListUtils.isDuplicate(old, key);
        BanUtils.saveToYaml(player, ClickType.ADD, old, BanType.WORLD);
    }

    public static boolean checkBan(ResourceLocation resourceLocation) {
        if (!YouerConfig.yml.getBoolean("bans.world", false)) return false;
        CACHE.add(resourceLocation);
        var list = BanConfig.getListByType(BanType.WORLD);
        if (list.isEmpty()) return false;
        return list.contains(resourceLocation.toString());
    }
}
