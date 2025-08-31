package com.mohistmc.youer.plugins.ban.bans;

import com.mohistmc.tools.ListUtils;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.plugins.ban.BanConfig;
import com.mohistmc.youer.plugins.ban.BanType;
import com.mohistmc.youer.plugins.ban.ClickType;
import com.mohistmc.youer.plugins.ban.utils.BanUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Player;

public class BanRecipe {

    public static Set<ResourceLocation> CACHE = new HashSet<>();

    public static void addBan(Player player, String key) {
        List<String> old = BanConfig.RECIPE.getRecipe();
        ListUtils.isDuplicate(old, key);
        BanUtils.saveToYaml(player, ClickType.ADD, old, BanType.RECIPE);
    }

    public static boolean checkBan(ResourceLocation resourceLocation) {
        if (!YouerConfig.yml.getBoolean("bans.recipe", false)) return false;
        CACHE.add(resourceLocation);
        return BanConfig.RECIPE.getRecipe().contains(resourceLocation.toString());
    }
}
