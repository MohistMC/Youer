package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V2502 {

    private static final int VERSION = MCVersions.V1_15_2 + 272;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:hoglin");
    }

    private V2502() {}
}
