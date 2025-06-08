package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V2522 {

    private static final int VERSION = MCVersions.V20W13B + 1;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:zoglin");
    }

    private V2522() {}
}
