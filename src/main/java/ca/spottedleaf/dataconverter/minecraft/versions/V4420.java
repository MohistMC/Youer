package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.util.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;

public final class V4420 {

    private static final int VERSION = MCVersions.V1_21_5 + 95;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:area_effect_cloud", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "Particle", "custom_particle");
                return null;
            }
        });
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:area_effect_cloud", new DataWalkerTypePaths<>(MCTypeRegistry.PARTICLE, "custom_particle"));
    }

    private V4420() {}
}
