package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.util.ComponentUtils;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4054 {

    private static final int VERSION = MCVersions.V1_21_1 + 99;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:banner", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                convertComponents(data.getMap("components"));
                return null;
            }
        });
        MCTypeRegistry.ITEM_STACK.addConverterForId("minecraft:white_banner", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                convertComponents(data.getMap("components"));
                return null;
            }
        });
    }

    private static void convertComponents(final MapType components) {
        if (components == null) {
            return;
        }

        final String itemNameKey = ComponentUtils.retrieveTranslationString(components.getString("minecraft:item_name"));

        if (!"block.minecraft.ominous_banner".equals(itemNameKey)) {
            return;
        }

        components.setString("minecraft:rarity", "uncommon");
        components.setString("minecraft:item_name", ComponentUtils.createTranslatableComponent("block.minecraft.ominous_banner"));
    }

    private V4054() {}
}
