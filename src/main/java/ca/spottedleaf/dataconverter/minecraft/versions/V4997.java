package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.chunk.ConverterAddBlendingData;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class V4997 {

    private static final int VERSION = MCVersions.V26_2 + 94;

    public static void register() {
        // See V3088 for why this converter is duplicated in multiple versions.
        MCTypeRegistry.CHUNK.addStructureConverter(new ConverterAddBlendingData(VERSION));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:poplar_chest_boat", new DataWalkerItemLists("Items"));
    }

    private V4997() {}
}
