package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;

public final class V3685 {

    private static final int VERSION = MCVersions.V23W42A + 1;

    private static String getType(final MapType<String> arrow) {
        return "minecraft:empty".equals(arrow.getString("Potion", "minecraft:empty")) ? "minecraft:arrow" : "minecraft:tipped_arrow";
    }

    private static MapType<String> createItem(final TypeUtil util, final String id, final int count) {
        final MapType<String> ret = util.createEmptyMap();

        ret.setString("id", id);
        ret.setInt("Count", count);

        return ret;
    }

    private static void registerArrowEntity(final String id) {
        MCTypeRegistry.ENTITY.addWalker(VERSION, id, new DataWalkerTypePaths<>(MCTypeRegistry.BLOCK_STATE, "inBlockState"));
        // new: item
        MCTypeRegistry.ENTITY.addWalker(VERSION, id, new DataWalkerItems("item"));
    }

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:trident", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "Trident", "item");
                return null;
            }
        });
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:arrow", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                data.setMap("item", createItem(data.getTypeUtil(), getType(data), 1));
                return null;
            }
        });
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:spectral_arrow", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                data.setMap("item", createItem(data.getTypeUtil(), "minecraft:spectral_arrow", 1));
                return null;
            }
        });

        registerArrowEntity("minecraft:trident");
        registerArrowEntity("minecraft:spectral_arrow");
        registerArrowEntity("minecraft:arrow");
    }

    private V3685() {}
}
