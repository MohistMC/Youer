package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V3816 {

    private static final int VERSION = MCVersions.V24W06A + 1;

    public static void register() {
        V100.registerEquipment(VERSION, "minecraft:bogged");
    }

    private V3816() {}
}
