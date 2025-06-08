package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V2671 {

    private static final int VERSION = MCVersions.V1_16_5 + 85;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:goat");
    }

    private V2671() {}
}
