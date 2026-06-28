package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4181 {

    private static final int VERSION = MCVersions.V1_21_4_PRE1 + 2;

    public static void register() {
        final DataConverter<MapType, MapType> furnaceConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "CookTime", "cooking_time_spent");
                RenameHelper.renameSingle(data, "CookTimeTotal", "cooking_total_time");
                RenameHelper.renameSingle(data, "BurnTime", "lit_time_remaining");

                final Object litTotalTime = data.getGeneric("lit_time_remaining");
                if (litTotalTime != null) {
                    data.setGeneric("lit_total_time", litTotalTime);
                }

                return null;
            }
        };

        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:furnace", furnaceConverter);
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:smoker", furnaceConverter);
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:blast_furnace", furnaceConverter);
    }

    private V4181() {}
}
