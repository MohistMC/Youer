package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;

public final class V3682 {

    private static final int VERSION = MCVersions.V23W41A + 1;

    public static void register() {
        V1458.namedInventory(VERSION, "minecraft:crafter");
    }

    private V3682() {}
}
