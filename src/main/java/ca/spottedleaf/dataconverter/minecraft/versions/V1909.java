package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;

public final class V1909 {

    private static final int VERSION = MCVersions.V18W45A + 1;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:jigsaw", new DataWalkerTypePaths<>(MCTypeRegistry.FLAT_BLOCK_STATE, "final_state"));
    }

    private V1909() {}
}
