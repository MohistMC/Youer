package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V1914 {

    private static final int VERSION = MCVersions.V18W48A;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:chest", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String lootTable = data.getString("LootTable");

                if ("minecraft:chests/village_blacksmith".equals(lootTable)) {
                    data.setString("LootTable", "minecraft:chests/village/village_weaponsmith");
                }

                return null;
            }
        });
    }

    private V1914() {}
}
