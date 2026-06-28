package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.chunk.ConverterAddBlendingData;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V4295 {

    private static final int VERSION = MCVersions.V1_21_4 + 106;

    public static void register() {
        // See V3088 for why this converter is duplicated in here, V4185, V3441, and V3088
        MCTypeRegistry.CHUNK.addStructureConverter(new ConverterAddBlendingData(VERSION));
    }

    private V4295() {}
}
