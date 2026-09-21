package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.leveldat.ConverterRemoveFeatureFlag;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import java.util.Arrays;
import java.util.HashSet;

public final class V4180 {

    private static final int VERSION = MCVersions.V1_21_4_PRE1 + 1;

    public static void register() {
        MCTypeRegistry.LIGHTWEIGHT_LEVEL.addStructureConverter(new ConverterRemoveFeatureFlag(VERSION, new HashSet<>(
            Arrays.asList(
                "minecraft:winter_drop"
            )
        )));
    }

    private V4180() {}
}
