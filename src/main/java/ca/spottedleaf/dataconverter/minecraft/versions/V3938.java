package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;

public final class V3938 {

    private static final int VERSION = MCVersions.V1_20_6 + 99;

    private static void registerArrow(final String id) {
        MCTypeRegistry.ENTITY.addWalker(VERSION, id, new DataWalkerTypePaths<>(MCTypeRegistry.BLOCK_STATE, "inBlockState"));
        // new: weapon
        MCTypeRegistry.ENTITY.addWalker(VERSION, id, new DataWalkerItems("item", "weapon"));
    }

    public static void register() {
        registerArrow("minecraft:spectral_arrow");
        registerArrow("minecraft:arrow");
    }

    private V3938() {}
}
