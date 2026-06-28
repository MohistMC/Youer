package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerListPaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;

public final class V4302 {

    private static final int VERSION = MCVersions.V25W02A + 4;

    public static void register() {
        // test_block is simple TE
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:test_instance_block", new DataWalkerTypePaths<>(MCTypeRegistry.TEXT_COMPONENT, "data"));
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:test_instance_block", new DataWalkerListPaths<>(MCTypeRegistry.TEXT_COMPONENT, "errors"));
    }

    private V4302() {}
}
