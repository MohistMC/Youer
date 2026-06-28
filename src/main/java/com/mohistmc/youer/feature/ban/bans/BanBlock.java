package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.ban.BanType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2025/11/1 19:00:23
 */
public class BanBlock {

    public static boolean check(BlockState p_46606_) {
        if (!YouerConfig.ban_block_enable) return false;
        if (p_46606_.isAir()) return false;
        var list = BanConfig.getListByType(BanType.BLOCK);
        if (list.isEmpty()) return false;
        Block block = p_46606_.getBlock();
        ItemStack bukkitBlock = block.asItem().getDefaultInstance().getBukkitStack();
        return list.contains(bukkitBlock.getType().key().asString());
    }
}
