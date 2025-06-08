package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;

public final class V3689 {

    private static final int VERSION = MCVersions.V23W44A + 1;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:breeze");
        // minecraft:wind_charge is a simple entity
        // minecraft:breeze_wind_charge is a simple entity

        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:trial_spawner", (final MapType<String> data, final long fromVersion, final long toVersion) -> {
            final ListType spawnPotentials = data.getList("spawn_potentials", ObjectType.MAP);
            if (spawnPotentials != null) {
                for (int i = 0, len = spawnPotentials.size(); i < len; ++i) {
                    WalkerUtils.convert(MCTypeRegistry.ENTITY, spawnPotentials.getMap(i).getMap("data"), "entity", fromVersion, toVersion);
                }
            }

            WalkerUtils.convert(MCTypeRegistry.ENTITY, data.getMap("spawn_data"), "entity", fromVersion, toVersion);
            return null;
        });
    }

    private V3689() {}
}
