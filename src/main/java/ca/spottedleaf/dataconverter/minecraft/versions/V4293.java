package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4293 {

    private static final int VERSION = MCVersions.V1_21_4 + 104;

    public static void register() {
        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            private static final float DEFAULT = 0.085f;

            private static final String[] ARMOR_SLOTS = new String[] {
                "feet",
                "legs",
                "chest",
                "head"
            };

            private static final String[] HAND_SLOTS = new String[] {
                "mainhand",
                "offhand"
            };

            private static void convertDropChances(final MapType root, final String srcPath, final String[] names, final MapType dst) {
                final ListType oldChances = root.getListUnchecked(srcPath);

                if (oldChances == null) {
                    return;
                }

                for (int i = 0, len = Math.min(oldChances.size(), names.length); i < len; ++i) {
                    final float chance = oldChances.getFloat(i, DEFAULT);
                    if (chance != DEFAULT) {
                        dst.setFloat(names[i], chance);
                    }
                }
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType dropChances = data.getTypeUtil().createEmptyMap();

                convertDropChances(data, "ArmorDropChances", ARMOR_SLOTS, dropChances);
                convertDropChances(data, "HandDropChances", HAND_SLOTS, dropChances);

                data.remove("ArmorDropChances");
                data.remove("HandDropChances");

                final float body = data.getFloat("body_armor_drop_chance", DEFAULT);
                data.remove("body_armor_drop_chance");

                if (body != DEFAULT) {
                    dropChances.setFloat("body", body);
                }

                if (!dropChances.isEmpty()) {
                    data.setMap("drop_chances", dropChances);
                }

                return null;
            }
        });
    }

    private V4293() {}
}
