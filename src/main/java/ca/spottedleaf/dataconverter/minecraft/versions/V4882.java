package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.chunk.ConverterAddBlendingData;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V4882 {

    private static final int VERSION = MCVersions.V26_2_SNAPSHOT1 - 1;

    public static void register() {
        // See V3088 for why this converter is duplicated in multiple versions
        MCTypeRegistry.CHUNK.addStructureConverter(new ConverterAddBlendingData(VERSION));
    }

    private V4882() {}
}
