package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import org.slf4j.Logger;

public final class V108 {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int VERSION = MCVersions.V15W32C + 4;

    public static void register() {
        // Convert String UUID into UUIDMost and UUIDLeast
        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String uuidString = data.getString("UUID");

                if (uuidString == null) {
                    return null;
                }
                data.remove("UUID");

                final UUID uuid;
                try {
                    uuid = UUID.fromString(uuidString);
                } catch (final Exception ex) {
                    LOGGER.warn("Failed to parse UUID for legacy entity (V108): " + uuidString, ex);
                    return null;
                }

                data.setLong("UUIDMost", uuid.getMostSignificantBits());
                data.setLong("UUIDLeast", uuid.getLeastSignificantBits());

                return null;
            }
        });
    }

    private V108() {}
}
