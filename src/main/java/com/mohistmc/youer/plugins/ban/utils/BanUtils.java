package com.mohistmc.youer.plugins.ban.utils;

import com.mohistmc.youer.plugins.ban.BanConfig;
import com.mohistmc.youer.plugins.ban.BanType;
import com.mohistmc.youer.plugins.ban.ClickType;
import com.mohistmc.youer.util.I18n;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 15:10:47
 */
public class BanUtils {

    public static void saveToYaml(Player player, ClickType clickType, List<String> list, BanType banType) {
        switch (banType) {
            case ITEM -> BanConfig.ITEM.put(banType.key, list);
            case ENTITY -> BanConfig.ENTITY.put(banType.key, list);
            case ENCHANTMENT -> BanConfig.ENCHANTMENT.put(banType.key, list);
            case ITEM_MOSHOU -> BanConfig.MOSHOU.put(banType.key, list);
            case RECIPE -> BanConfig.RECIPE.put(banType.key, list);
            case BLOCK -> BanConfig.BLOCK.put(banType.key, list);
        }
        if (clickType == ClickType.ADD) {
            player.sendMessage(I18n.as(banType.i18n_key_add));
        } else if (clickType == ClickType.REMOVE) {
            player.sendMessage(I18n.as(banType.i18n_key_remove));
        }
    }
}
