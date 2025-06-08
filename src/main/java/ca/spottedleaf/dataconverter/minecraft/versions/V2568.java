package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V2568 {

    private static final int VERSION = MCVersions.V1_16_1 + 1;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:piglin_brute");
    }

    private V2568() {}
}
