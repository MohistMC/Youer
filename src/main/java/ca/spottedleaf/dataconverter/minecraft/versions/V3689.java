package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.datatypes.DataWalker;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;

public final class V3689 {

    private static final int VERSION = MCVersions.V23W44A + 1;

    public static void register() {
        //registerMob("minecraft:breeze"); // changed to simple in 1.21.5
        // minecraft:wind_charge is a simple entity
        // minecraft:breeze_wind_charge is a simple entity

        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:trial_spawner", new DataWalker<>() {
            @Override
            public MapType walk(final MapType data, final long fromVersion, final long toVersion) {
                WalkerUtils.convertListPath(MCTypeRegistry.ENTITY, data, "spawn_potentials", "data", "entity", fromVersion, toVersion);

                WalkerUtils.convert(MCTypeRegistry.ENTITY, data.getMap("spawn_data"), "entity", fromVersion, toVersion);
                return null;
            }
        });
    }

    private V3689() {}
}
