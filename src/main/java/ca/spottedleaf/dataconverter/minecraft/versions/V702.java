package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.datatypes.DataWalker;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;

public final class V702 {

    private static final int VERSION = MCVersions.V1_10_2 + 190;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("Zombie", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final int zombieType = data.getInt("ZombieType");
                data.remove("ZombieType");

                switch (zombieType) {
                    case 0:
                    default:
                        break;

                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        data.setString("id", "ZombieVillager");
                        data.setInt("Profession", zombieType - 1);
                        break;

                    case 6:
                        data.setString("id", "Husk");
                        break;
                }

                return null;
            }
        });

        MCTypeRegistry.ENTITY.addWalker(VERSION, "ZombieVillager", new DataWalker<>() {
            @Override
            public MapType walk(final MapType root, final long fromVersion, final long toVersion) {
                WalkerUtils.convertList(MCTypeRegistry.VILLAGER_TRADE, root.getMap("Offers"), "Recipes", fromVersion, toVersion);
                return null;
            }
        });
        //registerMob("Husk"); // is now simple in 1.21.5
    }

    private V702() {}
}
