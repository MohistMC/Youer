package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.util.ComponentUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V3439 {

    private static final int VERSION = MCVersions.V1_19_4 + 102;

    public static void register() {
        final DataConverter<MapType<String>, MapType<String>> signTileUpdater = new DataConverter<>(VERSION) {
            private static final String DEFAULT_COLOR = "black";

            private static ListType migrateToList(final MapType<String> root, final String prefix) {
                if (root == null) {
                    return null;
                }

                final ListType ret = root.getTypeUtil().createEmptyList();

                ret.addString(root.getString(prefix.concat("1"), ComponentUtils.EMPTY));
                ret.addString(root.getString(prefix.concat("2"), ComponentUtils.EMPTY));
                ret.addString(root.getString(prefix.concat("3"), ComponentUtils.EMPTY));
                ret.addString(root.getString(prefix.concat("4"), ComponentUtils.EMPTY));

                return ret;
            }

            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                // front text
                final MapType<String> frontText = data.getTypeUtil().createEmptyMap();
                data.setMap("front_text", frontText);

                final ListType frontMessages = migrateToList(data, "Text");
                frontText.setList("messages", frontMessages);

                ListType frontFilteredMessages = null;

                for (int i = 0; i < 4; ++i) {
                    final String filtered = data.getString("FilteredText" + i);
                    if (filtered == null) {
                        if (frontFilteredMessages != null) {
                            frontFilteredMessages.addString(frontMessages.getString(i));
                        }
                        continue;
                    }

                    if (frontFilteredMessages == null) {
                        frontFilteredMessages = data.getTypeUtil().createEmptyList();
                        for (int k = 0; k < i; ++k) {
                            frontFilteredMessages.addString(frontMessages.getString(k));
                        }
                    }

                    frontFilteredMessages.addString(filtered);
                }

                if (frontFilteredMessages != null) {
                    frontText.setList("filtered_messages", frontFilteredMessages);
                }

                frontText.setString("color", data.getString("Color", DEFAULT_COLOR));
                frontText.setBoolean("has_glowing_text", data.getBoolean("GlowingText", false));
                frontText.setBoolean("_filtered_correct", true);

                // back text
                final MapType<String> backText = data.getTypeUtil().createEmptyMap();
                data.setMap("back_text", backText);

                final ListType blankMessages = data.getTypeUtil().createEmptyList();
                backText.setList("messages", blankMessages);

                for (int i = 0; i < 4; ++i) {
                    blankMessages.addString(ComponentUtils.EMPTY);
                }

                backText.setString("color", DEFAULT_COLOR);
                backText.setBoolean("has_glowing_text", false);

                // misc
                data.setBoolean("is_waxed", false);
                return null;
            }
        };

        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:sign", signTileUpdater);
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:hanging_sign", signTileUpdater);
    }

    private V3439() {}
}
