package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V4071 {

    private static final int VERSION = MCVersions.V24W39A + 2;

    public static void register() {
        //registerMob("minecraft:creaking"); // changed to simple in 1.21.5
        //registerMob("minecraft:creaking_transient"); // changed to simple in 1.21.5

        // minecraft:creaking_heart is a simple tile entity
    }

    private V4071() {}
}
