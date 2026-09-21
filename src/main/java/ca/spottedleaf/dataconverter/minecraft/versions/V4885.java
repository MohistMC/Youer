package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.tileentity.ConverterAbstractTileEntityRemove;

public final class V4885 {

    private static final int VERSION = MCVersions.V26_2_SNAPSHOT2 + 1;

    public static void register() {
        ConverterAbstractTileEntityRemove.register(VERSION, "minecraft:bed");
    }

    private V4885() {}
}
