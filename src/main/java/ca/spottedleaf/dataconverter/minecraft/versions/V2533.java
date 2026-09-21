package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V2533 {

    private static final int VERSION = MCVersions.V20W18A + 1;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:villager", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ListType attributes = data.getList("Attributes", ObjectType.MAP);

                if (attributes == null) {
                    return null;
                }

                for (int i = 0, len = attributes.size(); i < len; ++i) {
                    final MapType attribute = attributes.getMap(i);

                    if (!"generic.follow_range".equals(attribute.getString("Name"))) {
                        continue;
                    }

                    if (attribute.getDouble("Base") == 16.0) {
                        attribute.setDouble("Base", 48.0);
                    }
                }

                return null;
            }
        });
    }

    private V2533() {}
}
