package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.leveldat.ConverterRemoveFeatureFlag;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import java.util.Arrays;
import java.util.HashSet;

public final class V3939 {

    private static final int VERSION = MCVersions.V1_20_6 + 100;

    public static void register() {
        MCTypeRegistry.LEVEL.addStructureConverter(new ConverterRemoveFeatureFlag(VERSION, new HashSet<>(
            Arrays.asList(
                "minecraft:update_1_21"
            )
        )));
    }

    private V3939() {}
}
