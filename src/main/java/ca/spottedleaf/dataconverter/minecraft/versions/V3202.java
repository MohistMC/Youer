package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public final class V3202 {

    private static final int VERSION = MCVersions.V1_19_2 + 82;

    public static void register() {
        V99.registerSign(VERSION, "minecraft:hanging_sign");
    }

    private V3202() {}
}
