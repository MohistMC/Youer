package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.leveldat.ConverterRemoveFeatureFlag;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import java.util.Arrays;
import java.util.HashSet;

public final class V4424 {

    private static final int VERSION = MCVersions.V25W15A + 2;

    public static void register() {
        MCTypeRegistry.LIGHTWEIGHT_LEVEL.addStructureConverter(new ConverterRemoveFeatureFlag(VERSION, new HashSet<>(
            Arrays.asList(
                "minecraft:locator_bar"
            )
        )));
        final DataConverter<MapType, MapType> locatorBarConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType locatorBarIcon = data.getMap("locator_bar_icon");
                if (locatorBarIcon == null) {
                    return null;
                }

                locatorBarIcon.setString("style", "minecraft:default");
                return null;
            }
        };
        MCTypeRegistry.ENTITY.addStructureConverter(locatorBarConverter);
        MCTypeRegistry.PLAYER.addStructureConverter(locatorBarConverter);
    }

    private V4424() {}
}
