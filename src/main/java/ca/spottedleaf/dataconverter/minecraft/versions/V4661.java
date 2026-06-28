package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4661 {

    private static final int VERSION = MCVersions.V25W45A + 1;

    public static void register() {
        MCTypeRegistry.OPTIONS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (data.hasKey("showNowPlayingToast")) {
                    data.setString("musicToast", "false".equals(data.getString("showNowPlayingToast", "false")) ? "never" : "pause_and_toast");

                    data.remove("showNowPlayingToast");
                }

                return null;
            }
        });
    }

    private V4661() {}
}
