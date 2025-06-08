package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class V3566 {

    private static final int VERSION = MCVersions.V1_20_1 + 101;

    public static void register() {
        MCTypeRegistry.SAVED_DATA_SCOREBOARD.addStructureConverter(new DataConverter<>(VERSION) {

            private static final Map<String, String> SLOT_RENAMES = new HashMap<>(
                    ImmutableMap.<String, String>builder()
                            .put("slot_0", "list")
                            .put("slot_1", "sidebar")
                            .put("slot_2", "below_name")
                            .put("slot_3", "sidebar.team.black")
                            .put("slot_4", "sidebar.team.dark_blue")
                            .put("slot_5", "sidebar.team.dark_green")
                            .put("slot_6", "sidebar.team.dark_aqua")
                            .put("slot_7", "sidebar.team.dark_red")
                            .put("slot_8", "sidebar.team.dark_purple")
                            .put("slot_9", "sidebar.team.gold")
                            .put("slot_10", "sidebar.team.gray")
                            .put("slot_11", "sidebar.team.dark_gray")
                            .put("slot_12", "sidebar.team.blue")
                            .put("slot_13", "sidebar.team.green")
                            .put("slot_14", "sidebar.team.aqua")
                            .put("slot_15", "sidebar.team.red")
                            .put("slot_16", "sidebar.team.light_purple")
                            .put("slot_17", "sidebar.team.yellow")
                            .put("slot_18", "sidebar.team.white")
                            .build()
            );

            @Override
            public MapType<String> convert(final MapType<String> root, final long sourceVersion, final long toVersion) {
                final MapType<String> data = root.getMap("data");
                if (data == null) {
                    return null;
                }

                RenameHelper.renameKeys(data.getMap("DisplaySlots"), SLOT_RENAMES::get);

                return null;
            }
        });
    }

    private V3566() {}
}
