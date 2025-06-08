package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;

public final class V1470 {

    private static final int VERSION = MCVersions.V18W08A;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:turtle");
        registerMob("minecraft:cod_mob");
        registerMob("minecraft:tropical_fish");
        registerMob("minecraft:salmon_mob");
        registerMob("minecraft:puffer_fish");
        registerMob("minecraft:phantom");
        registerMob("minecraft:dolphin");
        registerMob("minecraft:drowned");

        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:trident", new DataWalkerTypePaths<>(MCTypeRegistry.BLOCK_STATE, "inBlockState"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:trident", new DataWalkerItems("Trident"));
    }

    private V1470() {}
}
