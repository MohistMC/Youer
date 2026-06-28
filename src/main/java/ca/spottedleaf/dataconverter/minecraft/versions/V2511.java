package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.Types;

public final class V2511 {

    private static final int VERSION = MCVersions.V20W09A + 1;

    private static int[] createUUIDArray(final long most, final long least) {
        return new int[] {
                (int)(most >>> 32),
                (int)most,
                (int)(least >>> 32),
                (int)least
        };
    }

    private static void setUUID(final MapType data, final long most, final long least) {
        if (most != 0L && least != 0L) {
            data.setInts("OwnerUUID", createUUIDArray(most, least));
        }
    }

    public static void register() {
        final DataConverter<MapType, MapType> throwableConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType owner = data.getMap("owner");
                data.remove("owner");
                if (owner == null) {
                    return null;
                }

                setUUID(data, owner.getLong("M"), owner.getLong("L"));

                return null;
            }
        };
        final DataConverter<MapType, MapType> potionConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType potion = data.getMap("Potion");
                data.remove("Potion");

                data.setMap("Item", potion == null ? Types.NBT.createEmptyMap() : potion);

                return null;
            }
        };
        final DataConverter<MapType, MapType> llamaSpitConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType owner = data.getMap("Owner");
                data.remove("Owner");
                if (owner == null) {
                    return null;
                }

                setUUID(data, owner.getLong("OwnerUUIDMost"), owner.getLong("OwnerUUIDLeast"));

                return null;
            }
        };
        final DataConverter<MapType, MapType> arrowConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                setUUID(data, data.getLong("OwnerUUIDMost"), data.getLong("OwnerUUIDLeast"));

                data.remove("OwnerUUIDMost");
                data.remove("OwnerUUIDLeast");

                return null;
            }
        };

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:egg", throwableConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:ender_pearl", throwableConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:experience_bottle", throwableConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:snowball", throwableConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:potion", throwableConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:potion", potionConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:llama_spit", llamaSpitConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:arrow", arrowConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:spectral_arrow", arrowConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:trident", arrowConverter);

        // Vanilla uses version step 1, but there's no need to add a step here.
        // Note: Originally we have had this walker on step 0, as Vanilla accidentally left this walker out
        //       until 1.21.5
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:potion", new DataWalkerItems("Item"));
    }

    private V2511() {}
}
