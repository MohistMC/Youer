package com.mohistmc.youer.util;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.block.CraftLootable;
import org.bukkit.craftbukkit.util.CraftStructureTransformer;
import org.bukkit.craftbukkit.util.TransformerGeneratorAccess;

public class StructureTemplateMixinFix {

    public static AtomicReference<BlockState> blockStateAtomicReference = new AtomicReference<>();
    static ServerLevelAccessor wrappedAccess;
    static CraftStructureTransformer structureTransformer = null;

    public static ServerLevelAccessor init(ServerLevelAccessor p_230329_) {
        // CraftBukkit start
        if (wrappedAccess instanceof TransformerGeneratorAccess transformerAccess) {
            p_230329_ = transformerAccess.getHandle();
            structureTransformer = transformerAccess.getStructureTransformer();
            if (structureTransformer != null && !structureTransformer.canTransformBlocks()) {
                structureTransformer = null;
            }
        }
        return p_230329_;
        // CraftBukkit end
    }

    public static StructureTemplate.StructureBlockInfo transformBlock(ServerLevelAccessor p_230329_, StructureTemplate.StructureBlockInfo info, BlockPos blockpos, BlockState blockstate, RandomSource random) {
        // CraftBukkit start
        if (structureTransformer != null) {
            CraftBlockState craftBlockState = (CraftBlockState) CraftBlockStates.getBlockState(p_230329_, blockpos, blockstate, null);
            if (info.nbt() != null && craftBlockState instanceof CraftBlockEntityState<?> entityState) {
                entityState.loadData(info.nbt());
                if (craftBlockState instanceof CraftLootable<?> craftLootable) {
                    craftLootable.setSeed(random.nextLong());
                }
            }
            craftBlockState = structureTransformer.transformCraftState(craftBlockState);
            blockstate = craftBlockState.getHandle();
            blockStateAtomicReference.set(blockstate);
            info = new StructureTemplate.StructureBlockInfo(blockpos, blockstate, (craftBlockState instanceof CraftBlockEntityState<?> craftBlockEntityState ? craftBlockEntityState.getSnapshotNBT() : null));
        }
        return info;
        // CraftBukkit end
    }

    public static boolean structureTransformer() {
        return structureTransformer == null;
    }

    public static boolean hasBlockStateAtomicReference() {
        return blockStateAtomicReference.get() != null;
    }

    public static BlockState getBlockStateAtomicReference() {
        return blockStateAtomicReference.getAndSet(null);
    }
}
