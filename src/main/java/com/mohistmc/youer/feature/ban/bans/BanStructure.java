package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import com.mohistmc.youer.feature.ban.ClickType;
import com.mohistmc.youer.feature.ban.BanSaveInventory;
import com.mohistmc.youer.util.I18n;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.bukkit.entity.Player;

/**
 * @author Mgazul
 * @date 2026/7/2 02:40
 */
public class BanStructure {

    public static void addBan(Player player, String key) {
        List<String> old = BanConfig.getListByType(BanType.STRUCTURE);
        if (old.contains(key)) {
            player.sendMessage(I18n.as("banscmd.add.structure.exists"));
            return;
        }
        old.add(key);
        BanSaveInventory banSaveInventory = new BanSaveInventory(BanType.STRUCTURE, I18n.as("banscmd.gui.add.structure"));
        banSaveInventory.saveToYaml(player, ClickType.ADD, old, BanType.STRUCTURE);
    }

    public static boolean checkBan(Identifier resourceLocation) {
        if (!YouerConfig.ban_structure_enable) return false;
        var list = BanConfig.getListByType(BanType.STRUCTURE);
        if (list.isEmpty()) return false;
        return list.contains(resourceLocation.toString()) || list.contains(resourceLocation.getNamespace() + ":*");
    }
}
