package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;

public final class V2688 {

    private static final int VERSION = MCVersions.V20W51A + 1;

    public static void register() {
        //registerMob("minecraft:glow_squid"); // changed to simple in 1.21.5
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:glow_item_frame", new DataWalkerItems("Item"));
    }

    private V2688() {}
}
