package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.tools.ListUtils;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import com.mohistmc.youer.feature.ban.ClickType;
import com.mohistmc.youer.feature.ban.utils.BanUtils;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Player;

/**
 * @author Mgazul
 * @date 2026/7/2 02:40
 */
public class BanStructure {

    public static void addBan(Player player, String key) {
        List<String> old = BanConfig.getListByType(BanType.STRUCTURE);
        ListUtils.isDuplicate(old, key);
        BanUtils.saveToYaml(player, ClickType.ADD, old, BanType.STRUCTURE);
    }

    public static boolean checkBan(ResourceLocation resourceLocation) {
        if (!YouerConfig.yml.getBoolean("bans.structure", false)) return false;
        var list = BanConfig.getListByType(BanType.STRUCTURE);
        if (list.isEmpty()) return false;
        return list.contains(resourceLocation.toString()) || list.contains(resourceLocation.getNamespace() + ":*");
    }
}
