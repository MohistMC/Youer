package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V703 {

    private static final int VERSION = MCVersions.V1_10_2 + 191;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("EntityHorse", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final int type = data.getInt("Type");
                data.remove("Type");

                switch (type) {
                    case 0:
                    default:
                        data.setString("id", "Horse");
                        break;

                    case 1:
                        data.setString("id", "Donkey");
                        break;

                    case 2:
                        data.setString("id", "Mule");
                        break;

                    case 3:
                        data.setString("id", "ZombieHorse");
                        break;

                    case 4:
                        data.setString("id", "SkeletonHorse");
                        break;
                }

                return null;
            }
        });

        MCTypeRegistry.ENTITY.addWalker(VERSION, "Horse", new DataWalkerItems("ArmorItem", "SaddleItem"));
        V100.registerEquipment(VERSION, "Horse");

        MCTypeRegistry.ENTITY.addWalker(VERSION, "Donkey", new DataWalkerItems("SaddleItem"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "Donkey", new DataWalkerItemLists("Items"));
        V100.registerEquipment(VERSION, "Donkey");

        MCTypeRegistry.ENTITY.addWalker(VERSION, "Mule", new DataWalkerItems("SaddleItem"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "Mule", new DataWalkerItemLists("Items"));
        V100.registerEquipment(VERSION, "Mule");

        MCTypeRegistry.ENTITY.addWalker(VERSION, "ZombieHorse", new DataWalkerItems("SaddleItem"));
        V100.registerEquipment(VERSION, "ZombieHorse");

        MCTypeRegistry.ENTITY.addWalker(VERSION, "SkeletonHorse", new DataWalkerItems("SaddleItem"));
        V100.registerEquipment(VERSION, "SkeletonHorse");
    }

    private V703() {}
}
