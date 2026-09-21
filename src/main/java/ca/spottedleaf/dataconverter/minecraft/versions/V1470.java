package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;

public final class V1470 {

    private static final int VERSION = MCVersions.V18W08A;

    public static void register() {
        //registerMob("minecraft:turtle"); // is now simple in 1.21.5
        //registerMob("minecraft:cod_mob"); // is now simple in 1.21.5
        //registerMob("minecraft:tropical_fish"); // is now simple in 1.21.5
        //registerMob("minecraft:salmon_mob"); // is now simple in 1.21.5
        //registerMob("minecraft:puffer_fish"); // is now simple in 1.21.5
        //registerMob("minecraft:phantom"); // is now simple in 1.21.5
        //registerMob("minecraft:dolphin"); // is now simple in 1.21.5
        //registerMob("minecraft:drowned"); // is now simple in 1.21.5

        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:trident", new DataWalkerTypePaths<>(MCTypeRegistry.BLOCK_STATE, "inBlockState"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:trident", new DataWalkerItems("Trident"));
    }

    private V1470() {}
}
