package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V1801 {

    private static final int VERSION = MCVersions.V1_13_2 + 170;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:illager_beast");
    }

    private V1801() {}
}
