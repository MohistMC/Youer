package com.mohistmc.youer.plugins.ban.bans;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.plugins.ban.BanConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2025/11/1 19:00:23
 */
public class BanBlock {

    public static boolean check(BlockState p_46606_) {
        if (!YouerConfig.ban_block_enable) return false;
        if (p_46606_.isAir()) return false;
        Block block = p_46606_.getBlock();
        ItemStack bukkitBlock = block.asItem().getDefaultInstance().getBukkitStack();
        return BanConfig.BLOCK.getBlock().contains(bukkitBlock.getType().key().asString());
    }
}
