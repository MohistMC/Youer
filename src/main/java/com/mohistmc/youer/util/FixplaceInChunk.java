package com.mohistmc.youer.util;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.bukkit.craftbukkit.util.CraftStructureTransformer;
import org.bukkit.craftbukkit.util.TransformerGeneratorAccess;

/**
 * @author Mgazul
 * @date 2026/8/25 15:29
 */
public class FixplaceInChunk {

    // Youer start - thread-local transformer access for parallel world gen (C2ME)
    private static final ThreadLocal<TransformerGeneratorAccess> CURRENT_ACCESS = new ThreadLocal<>();

    public static TransformerGeneratorAccess getCurrentTransformerAccess() {
        return CURRENT_ACCESS.get();
    }

    public static void init(WorldGenLevel worldGenLevel, StructureManager structureManager, Structure structure, BoundingBox boundingBox, ChunkPos chunkPos, org.bukkit.event.world.AsyncStructureGenerateEvent.Cause cause) {
        TransformerGeneratorAccess access = new TransformerGeneratorAccess();
        access.setHandle(worldGenLevel);
        access.setStructureTransformer(new CraftStructureTransformer(cause, worldGenLevel, structureManager, structure, boundingBox, chunkPos));
        CURRENT_ACCESS.set(access);
    }

    public static void discard() {
        TransformerGeneratorAccess access = CURRENT_ACCESS.get();
        if (access != null) {
            CraftStructureTransformer transformer = access.getStructureTransformer();
            if (transformer != null) {
                transformer.discard();
            }
            CURRENT_ACCESS.remove();
        }
    }
    // Youer end
}
