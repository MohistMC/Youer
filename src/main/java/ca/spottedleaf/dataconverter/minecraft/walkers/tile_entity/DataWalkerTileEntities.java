package ca.spottedleaf.dataconverter.minecraft.walkers.tile_entity;

import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;

public final class DataWalkerTileEntities extends DataWalkerTypePaths<MapType, MapType> {

    public DataWalkerTileEntities(final String... paths) {
        super(MCTypeRegistry.TILE_ENTITY, paths);
    }
}
