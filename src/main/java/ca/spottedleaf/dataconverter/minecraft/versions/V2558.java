package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.options.ConverterAbstractOptionsRename;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

public final class V2558 {

    private static final int VERSION = MCVersions.V1_16_PRE2 + 1;

    public static void register() {
        ConverterAbstractOptionsRename.register(VERSION, new HashMap<>(
                ImmutableMap.of(
                        "key_key.swapHands", "key_key.swapOffhand"
                )
        )::get);

        MCTypeRegistry.WORLD_GEN_SETTINGS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType dimensions = data.getOrCreateMap("dimensions");

                if (dimensions.isEmpty()) {
                    data.setMap("dimensions", recreateSettings(data));
                }

                return null;
            }
        });
    }

    private static MapType recreateSettings(final MapType data) {
        final long seed = data.getLong("seed");

        return V2550.vanillaLevels(data.getTypeUtil(), seed, V2550.defaultOverworld(data.getTypeUtil(), seed), false);
    }

    private V2558() {}
}
