package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;

public final class V4070 {

    private static final int VERSION = MCVersions.V24W39A + 1;

    private static void registerChestBoat(final String id) {
        MCTypeRegistry.ENTITY.addWalker(VERSION, id, new DataWalkerItemLists("Items"));
    }

    public static void register() {
        // minecraft:pale_oak_boat is a simple entity

        registerChestBoat("minecraft:pale_oak_chest_boat");
    }

    private V4070() {}
}
