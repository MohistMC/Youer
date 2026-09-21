package ca.spottedleaf.dataconverter.minecraft.converters.attributes;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.converter.util.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import java.util.function.Function;

public final class ConverterAbstractOldAttributesRename {

    public static void register(final int version, final Function<String, String> renamer) {
        register(version, 0, renamer);
    }

    public static void register(final int version, final int versionStep, final Function<String, String> renamer) {
        final DataConverter<MapType, MapType> entityConverter = new DataConverter<>(version, versionStep) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ListType attributes = data.getList("Attributes", ObjectType.MAP);

                if (attributes == null) {
                    return null;
                }

                for (int i = 0, len = attributes.size(); i < len; ++i) {
                    RenameHelper.renameString(attributes.getMap(i), "Name", renamer);
                }

                return null;
            }
        };

        MCTypeRegistry.ENTITY.addStructureConverter(entityConverter);
        MCTypeRegistry.PLAYER.addStructureConverter(entityConverter);

        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(version, versionStep) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType tag = data.getMap("tag");

                if (tag == null) {
                    return null;
                }

                final ListType attributes = tag.getList("AttributeModifiers", ObjectType.MAP);

                if (attributes == null) {
                    return null;
                }

                for (int i = 0, len = attributes.size(); i < len; ++i) {
                    RenameHelper.renameString(attributes.getMap(i), "AttributeName", renamer);
                }

                return null;
            }
        });
    }

    private ConverterAbstractOldAttributesRename() {}
}
