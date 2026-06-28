package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;

public final class V4544 {

    private static final int VERSION = MCVersions.V25W35A + 2;

    public static void register() {
        MCTypeRegistry.LEVEL.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final TypeUtil<?> typeUtil = data.getTypeUtil();
                final MapType worldBorder = typeUtil.createEmptyMap();
                data.setMap("world_border", worldBorder);

                worldBorder.setDouble("center_x", data.getDouble("BorderCenterX", 0.0));
                worldBorder.setDouble("center_z", data.getDouble("BorderCenterZ", 0.0));
                worldBorder.setDouble("size", data.getDouble("BorderSize", (double)5.999997E7f));
                worldBorder.setLong("lerp_time", data.getLong("BorderSizeLerpTime", 0L));
                worldBorder.setDouble("lerp_target", data.getDouble("BorderSizeLerpTarget", 0.0));
                worldBorder.setDouble("safe_zone", data.getDouble("BorderSafeZone", 5.0));
                worldBorder.setDouble("damage_per_block", data.getDouble("BorderDamagePerBlock", 0.2));
                worldBorder.setInt("warning_blocks", data.getInt("BorderWarningBlocks", 5));
                worldBorder.setInt("warning_time", data.getInt("BorderWarningTime", 15));

                data.remove("BorderCenterX");
                data.remove("BorderCenterZ");
                data.remove("BorderSize");
                data.remove("BorderSizeLerpTime");
                data.remove("BorderSizeLerpTarget");
                data.remove("BorderSafeZone");
                data.remove("BorderDamagePerBlock");
                data.remove("BorderWarningBlocks");
                data.remove("BorderWarningTime");

                return null;
            }
        });
    }

    private V4544() {}
}
