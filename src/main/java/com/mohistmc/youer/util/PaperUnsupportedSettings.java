package com.mohistmc.youer.util;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PaperUnsupportedSettings {

    private static boolean allowDesync;
    private static BlockState iblockdata1;
    private static BlockPos oldPos;

    public static void start_allowPistonDuplication(BlockPos blockpos3, Level p_60182_) {
        // Paper start - fix a variety of piston desync dupes
        allowDesync = io.papermc.paper.configuration.GlobalConfiguration.get().unsupportedSettings.allowPistonDuplication;
        oldPos = blockpos3;
        BlockState iblockdata1 = allowDesync ? p_60182_.getBlockState(oldPos) : null;
        // Paper end - fix a variety of piston desync dupes
    }

    public static void end_allowPistonDuplication(BlockPos blockpos3, List<BlockPos> list, int k, Level p_60182_, Map<BlockPos, BlockState> map, BlockState blockstate8, List<BlockState> list1, Direction p_60184_, boolean p_60185_) {
        // Paper start - fix a variety of piston desync dupes
        if (!allowDesync) {
            iblockdata1 = p_60182_.getBlockState(oldPos);
            map.replace(oldPos, iblockdata1);
        }
        p_60182_.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockpos3, blockstate8, allowDesync ? list1.get(k) : iblockdata1, p_60184_, p_60185_, false));
        if (!allowDesync) {
            p_60182_.setBlock(oldPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON | 1024); // set air to prevent later physics updates from seeing this block
        }
        // Paper end - fix a variety of piston desync dupes
    }
}
