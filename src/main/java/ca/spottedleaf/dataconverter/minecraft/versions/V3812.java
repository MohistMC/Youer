package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;

public final class V3812 {

    private static final int VERSION = MCVersions.V24W05B + 1;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:wolf", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                boolean doubleHealth = false;

                final ListType attributes = data.getList("Attributes", ObjectType.MAP);
                if (attributes != null) {
                    for (int i = 0, len = attributes.size(); i < len; ++i) {
                        final MapType<String> attribute = attributes.getMap(i);

                        if (!"minecraft:generic.max_health".equals(NamespaceUtil.correctNamespace(attribute.getString("Name")))) {
                            continue;
                        }

                        final double base = attribute.getDouble("Base", 0.0D);
                        if (base == 20.0D) {
                            attribute.setDouble("Base", 40.0D);
                            doubleHealth = true;
                        }
                    }
                }

                if (doubleHealth) {
                    data.setFloat("Health", data.getFloat("Health", 0.0F) * 2.0F);
                }

                return null;
            }
        });
    }

    private V3812() {}
}
