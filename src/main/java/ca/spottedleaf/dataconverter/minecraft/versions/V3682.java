package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;

public final class V3682 {

    private static final int VERSION = MCVersions.V23W41A + 1;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:crafter", new DataWalkerItemLists("Items"));
    }

    private V3682() {}
}
