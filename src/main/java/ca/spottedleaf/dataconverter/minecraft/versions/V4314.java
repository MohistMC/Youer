package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.CopyHelper;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;

public final class V4314 {

    private static final int VERSION = MCVersions.V25W06A + 1;

    public static void register() {
        final DataConverter<MapType, MapType> livingEntityConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                V4309.convertBlockPosition(data, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
                return null;
            }
        };

        MCTypeRegistry.PLAYER.addStructureConverter(livingEntityConverter);
        MCTypeRegistry.PLAYER.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                final TypeUtil<?> typeUtil = root.getTypeUtil();

                final Number spawnX = root.getNumber("SpawnX");
                final Number spawnY = root.getNumber("SpawnY");
                final Number spawnZ = root.getNumber("SpawnZ");
                if (spawnX != null && spawnY != null && spawnZ != null) {
                    root.remove("SpawnX");
                    root.remove("SpawnY");
                    root.remove("SpawnZ");

                    final MapType respawn = typeUtil.createEmptyMap();
                    root.setMap("respawn", respawn);

                    respawn.setInts("pos", V4309.makeBlockPosition(spawnX, spawnY, spawnZ));

                    CopyHelper.move(root, "SpawnAngle", respawn, "angle");
                    CopyHelper.move(root, "SpawnDimension", respawn, "dimension");
                    CopyHelper.move(root, "SpawnForced", respawn, "forced");
                }

                final MapType netherPos = root.getMap("enteredNetherPosition");
                if (netherPos != null) {
                    root.remove("enteredNetherPosition");

                    final ListType newPos = typeUtil.createEmptyList();
                    root.setList("entered_nether_pos", newPos);

                    newPos.addDouble(netherPos.getDouble("x", 0.0));
                    newPos.addDouble(netherPos.getDouble("y", 0.0));
                    newPos.addDouble(netherPos.getDouble("z", 0.0));
                }
                return null;
            }
        });

        MCTypeRegistry.ENTITY.addStructureConverter(livingEntityConverter);

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:vex", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                V4309.convertBlockPosition(data, "BoundX", "BoundY", "BoundZ", "bound_pos");
                RenameHelper.renameSingle(data, "LifeTicks", "life_ticks");
                return null;
            }
        });
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:phantom", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                V4309.convertBlockPosition(data, "AX", "AY", "AZ", "anchor_pos");
                RenameHelper.renameSingle(data, "Size", "size");
                return null;
            }
        });
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:turtle", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.remove("TravelPosX");
                data.remove("TravelPosY");
                data.remove("TravelPosZ");
                V4309.convertBlockPosition(data, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
                RenameHelper.renameSingle(data, "HasEgg", "has_egg");
                return null;
            }
        });

        final DataConverter<MapType, MapType> attachedBlockConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                V4309.convertBlockPosition(data, "TileX", "TileY", "TileZ", "block_pos");
                return null;
            }
        };

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:item_frame", attachedBlockConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:glow_item_frame", attachedBlockConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:painting", attachedBlockConverter);
        // Note: Leash entities never wrote TileX/Y/Z and don't read block_pos... Why is this here then?
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:leash_knot", attachedBlockConverter);
    }

    private V4314() {}
}
