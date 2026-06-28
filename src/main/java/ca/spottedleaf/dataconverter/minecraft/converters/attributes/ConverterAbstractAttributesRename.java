package ca.spottedleaf.dataconverter.minecraft.converters.attributes;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import java.util.function.Function;

public final class ConverterAbstractAttributesRename {

    public static void register(final int version, final Function<String, String> renamer) {
        register(version, 0, renamer);
    }

    public static void register(final int version, final int versionStep, final Function<String, String> renamer) {
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(version, versionStep) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType attributeModifiers = data.getMap("minecraft:attribute_modifiers");
                if (attributeModifiers == null) {
                    return null;
                }

                final ListType modifiers = attributeModifiers.getList("modifiers", ObjectType.MAP);
                if (modifiers == null) {
                    return null;
                }

                for (int i = 0, len = modifiers.size(); i < len; ++i) {
                    RenameHelper.renameString(modifiers.getMap(i), "type", renamer);
                }

                return null;
            }
        });

        final DataConverter<MapType, MapType> entityConverter = new DataConverter<>(version, versionStep) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ListType modifiers = data.getList("attributes", ObjectType.MAP);
                if (modifiers == null) {
                    return null;
                }

                for (int i = 0, len = modifiers.size(); i < len; ++i) {
                    RenameHelper.renameString(modifiers.getMap(i), "id", renamer);
                }

                return null;
            }
        };

        MCTypeRegistry.ENTITY.addStructureConverter(entityConverter);
        MCTypeRegistry.PLAYER.addStructureConverter(entityConverter);
    }

    private ConverterAbstractAttributesRename() {}
}
