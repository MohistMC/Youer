package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V3799 {

    private static final int VERSION = MCVersions.V1_20_4 + 99;

    public static void register() {
        V100.registerEquipment(VERSION, "minecraft:armadillo");
    }

    private V3799() {}
}
