package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author Mgazul by MohistMC
 * @date 2025/11/1 19:00:23
 */
public class BanBlock {

    public static boolean check(BlockState p_46606_) {
        if (!YouerConfig.ban_block_enable) return false;
        if (p_46606_.isAir()) return false;
        // Youer - use cached HashSet (O(1)) and registry key instead of allocating a Bukkit ItemStack per setBlock
        var set = BanConfig.getSetByType(BanType.BLOCK);
        if (set.isEmpty()) return false;
        return set.contains(BuiltInRegistries.BLOCK.getKey(p_46606_.getBlock()).toString());
    }
}
