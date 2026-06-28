package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4548 {

    private static final int VERSION = MCVersions.V1_21_9_PRE1;

    public static void register() {
        MCTypeRegistry.LEVEL.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final int x = data.getInt("SpawnX");
                final int y = data.getInt("SpawnY");
                final int z = data.getInt("SpawnZ");
                final float yaw = data.getFloat("SpawnAngle");

                data.remove("SpawnX");
                data.remove("SpawnY");
                data.remove("SpawnZ");
                data.remove("SpawnAngle");

                final MapType spawn = data.getTypeUtil().createEmptyMap();
                data.setMap("spawn", spawn);

                spawn.setString("dimension", "minecraft:overworld");
                spawn.setInts("pos", new int[] { x, y, z });
                spawn.setFloat("yaw", yaw);
                spawn.setFloat("pitch", 0.0f);

                return null;
            }
        });

        MCTypeRegistry.PLAYER.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType respawn = data.getMap("respawn");

                if (respawn == null) {
                    return null;
                }

                final String dimension = respawn.getString("dimension");
                final float yaw = respawn.getFloat("angle");

                respawn.remove("angle");

                respawn.setFloat("yaw", yaw);
                respawn.setFloat("pitch", 0.0f);
                if (dimension == null) {
                    respawn.setString("dimension", "minecraft:overworld");
                }

                return null;
            }
        });
    }

    private V4548() {}
}
