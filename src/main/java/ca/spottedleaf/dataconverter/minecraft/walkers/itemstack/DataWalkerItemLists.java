package ca.spottedleaf.dataconverter.minecraft.walkers.itemstack;

import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerListPaths;
import ca.spottedleaf.dataconverter.types.MapType;

public class DataWalkerItemLists extends DataWalkerListPaths<MapType, MapType> {

    public DataWalkerItemLists(final String... paths) {
        super(MCTypeRegistry.ITEM_STACK, paths);
    }
}
