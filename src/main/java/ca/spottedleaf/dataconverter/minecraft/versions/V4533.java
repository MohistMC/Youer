package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;

public final class V4533 {

    private static final int VERSION = MCVersions.V1_21_8 + 93;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:shelf", new DataWalkerItemLists("Items"));
    }

    private V4533() {}
}
