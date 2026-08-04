package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.EntityAPI;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * @author Mgazul by MohistMC
 * @date 2023/7/27 13:35:20
 */
public class BanEntity {

    public static boolean check(Entity entity) {
        if (!YouerConfig.ban_entity_enable) return false;
        if (entity instanceof Player) return false;
        if (entity instanceof FakePlayer) return false;
        if (entity instanceof ArmorStand) return false;
        if (!(entity instanceof LivingEntity)) return false;
        if (entity.getBukkitEntity().hasMetadata("npc")) return false;
        // Youer start - early exit when neither ban path can match
        Set<String> whitelist = YouerConfig.no_vanilla_entity_whitelist;
        Set<String> banList = BanConfig.ENTITY == null ? null : BanConfig.getSetByType(BanType.ENTITY);
        if ((!YouerConfig.no_vanilla_entity_enable || whitelist.isEmpty()) && (banList == null || banList.isEmpty())) {
            return false;
        }
        // Youer end
        return banVanilla(entity) || EntityAPI.isBan(entity);
    }

    /**
     * Checks if a vanilla entity should be banned
     *
     * @param entity The entity to check
     * @return true if the entity should be banned, false otherwise
     */
    public static boolean banVanilla(Entity entity) {
        if (!YouerConfig.no_vanilla_entity_enable) return false;
        var list = YouerConfig.no_vanilla_entity_whitelist;
        if (list.isEmpty()) return false;
        String key = EntityAPI.resourceLocation(entity);
        return key.startsWith("minecraft:") && !list.contains(key);
    }
}
