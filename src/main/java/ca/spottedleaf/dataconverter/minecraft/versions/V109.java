package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V109 {

    private static final int VERSION = MCVersions.V15W32C + 5;

    public static void register() {
        // Converts health to be in float, and cleans up whatever the hell was going on with HealF and Health...
        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final Number healF = data.getNumber("HealF");
                final Number heal = data.getNumber("Health");

                final float newHealth;

                if (healF != null) {
                    data.remove("HealF");
                    newHealth = healF.floatValue();
                } else {
                    if (heal == null) {
                        return null;
                    }

                    newHealth = heal.floatValue();
                }

                data.setFloat("Health", newHealth);

                return null;
            }
        });
    }

    private V109() {}
}
