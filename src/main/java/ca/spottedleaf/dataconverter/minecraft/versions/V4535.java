package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4535 {

    private static final int VERSION = MCVersions.V25W31A + 1;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:copper_golem", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                switch (data.getInt("weather_state", 0)) {
                    case 1: {
                        data.setString("weather_state", "exposed");
                        break;
                    }
                    case 2: {
                        data.setString("weather_state", "weathered");
                        break;
                    }
                    case 3: {
                        data.setString("weather_state", "oxidized");
                        break;
                    }
                    default: {
                        data.setString("weather_state", "unaffected");
                        break;
                    }
                }
                return null;
            }
        });
    }

    private V4535() {}
}
