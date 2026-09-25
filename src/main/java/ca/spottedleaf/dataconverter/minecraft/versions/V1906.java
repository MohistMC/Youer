package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;

public final class V1906 {

    private static final int VERSION = MCVersions.V18W43C + 3;

    public static void register() {
        V1458.namedInventory(VERSION, "minecraft:barrel");
        V1458.namedInventory(VERSION, "minecraft:smoker");
        V1458.namedInventory(VERSION, "minecraft:blast_furnace");
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:lectern", new DataWalkerItems("Book"));
    }

    private V1906() {}
}
