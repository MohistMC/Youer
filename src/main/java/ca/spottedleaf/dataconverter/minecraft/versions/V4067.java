package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.leveldat.ConverterRemoveFeatureFlag;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.types.MapType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class V4067 {

    private static final int VERSION = MCVersions.V24W38A + 1;

    private static final record BoatType(String name, String suffix) {}
    private static final BoatType[] BOAT_TYPES = new BoatType[] {
        new BoatType("oak", "boat"),
        new BoatType("spruce", "boat"),
        new BoatType("birch", "boat"),
        new BoatType("jungle", "boat"),
        new BoatType("acacia", "boat"),
        new BoatType("cherry", "boat"),
        new BoatType("dark_oak", "boat"),
        new BoatType("mangrove", "boat"),
        new BoatType("bamboo", "raft")
    };

    private static void registerChestBoat(final String id) {
        MCTypeRegistry.ENTITY.addWalker(VERSION, id, new DataWalkerItemLists("Items"));
    }

    public static void register() {
        // minecraft:oak_boat is a simple entity
        // minecraft:spruce_boat is a simple entity
        // minecraft:birch_boat is a simple entity
        // minecraft:jungle_boat is a simple entity
        // minecraft:acacia_boat is a simple entity
        // minecraft:cherry_boat is a simple entity
        // minecraft:dark_oak_boat is a simple entity
        // minecraft:mangrove_boat is a simple entity
        // minecraft:bamboo_raft is a simple entity

        registerChestBoat("minecraft:oak_chest_boat");
        registerChestBoat("minecraft:spruce_chest_boat");
        registerChestBoat("minecraft:birch_chest_boat");
        registerChestBoat("minecraft:jungle_chest_boat");
        registerChestBoat("minecraft:acacia_chest_boat");
        registerChestBoat("minecraft:cherry_chest_boat");
        registerChestBoat("minecraft:dark_oak_chest_boat");
        registerChestBoat("minecraft:mangrove_chest_boat");
        registerChestBoat("minecraft:bamboo_chest_raft");

        // we do not update V704 to set the new boat types, as the new ids are only registered here
        // if we updated V704 to set the new boat types, then the registered walkers for the chest_boat would not run before
        // V4067 as they are only registered here

        // to ensure that the id is correctly set eventually, we will force Type to the correct value based on the item id
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
            private static final Map<String, String> BOAT_TYPES_BY_ITEM_ID = new HashMap<>();
            static {
                for (final BoatType boatType : BOAT_TYPES) {
                    BOAT_TYPES_BY_ITEM_ID.put(boatType.name() + "_" + boatType.suffix(), boatType.name());
                    BOAT_TYPES_BY_ITEM_ID.put(boatType.name() + "_chest_" + boatType.suffix(), boatType.name());
                }
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String id = data.getString("id");
                final String boatType = BOAT_TYPES_BY_ITEM_ID.get(id);

                if (boatType == null) {
                    return null;
                }

                final MapType components = data.getMap("components");
                if (components == null) {
                    return null;
                }

                final MapType entityData = components.getMap("minecraft:entity_data");
                if (entityData == null) {
                    return null;
                }

                entityData.setString("Type", boatType);

                return null;
            }
        });

        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            private static final Map<String, String> NORMAL_REMAPPING = new HashMap<>(BOAT_TYPES.length);
            static {
                for (final BoatType type : BOAT_TYPES) {
                    NORMAL_REMAPPING.put(type.name(), type.name() + "_" + type.suffix());
                }
            }
            private static final Map<String, String> CHEST_REMAPPING = new HashMap<>(BOAT_TYPES.length);
            static {
                for (final BoatType type : BOAT_TYPES) {
                    CHEST_REMAPPING.put(type.name(), type.name() + "_chest_" + type.suffix());
                }
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String id = data.getString("id");
                if (id == null) {
                    // wat
                    return null;
                }

                final boolean normalBoat = id.equals("minecraft:boat");
                final boolean chestBoat = id.equals("minecraft:chest_boat");

                if (!normalBoat && !chestBoat) {
                    return null;
                }

                final String type = data.getString("Type");
                data.remove("Type");

                if (normalBoat) {
                    data.setString("id", NORMAL_REMAPPING.getOrDefault(type, "minecraft:oak_boat"));
                } else {
                    data.setString("id", CHEST_REMAPPING.getOrDefault(type, "minecraft:oak_chest_boat"));
                }

                return null;
            }
        });

        MCTypeRegistry.LIGHTWEIGHT_LEVEL.addStructureConverter(
            new ConverterRemoveFeatureFlag(VERSION, new HashSet<>(Arrays.asList("minecraft:bundle")))
        );
    }

    private V4067() {}
}
