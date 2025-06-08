package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;

public final class V3813 {

    private static final int VERSION = MCVersions.V24W05B + 2;

    private static final String[] PATROLLING_MOBS = new String[] {
            "minecraft:witch",
            "minecraft:ravager",
            "minecraft:pillager",
            "minecraft:illusioner",
            "minecraft:evoker",
            "minecraft:vindicator"
    };

    public static void register() {
        class RootPositionConverter extends DataConverter<MapType<String>, MapType<String>> {
            private final RenamePair[] convert;

            public RootPositionConverter(final int toVersion, final RenamePair[] convert) {
                this(toVersion, 0, convert);
            }

            public RootPositionConverter(final int toVersion, final int versionStep, final RenamePair[] convert) {
                super(toVersion, versionStep);
                this.convert = convert;
            }

            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                for (final RenamePair rename : this.convert) {
                    V3807.flattenBlockPos(data, rename.from);
                    RenameHelper.renameSingle(data, rename.from, rename.to);
                }
                return null;
            }
        }

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:bee", new RootPositionConverter(VERSION, new RenamePair[] {
                new RenamePair("HivePos", "hive_pos"),
                new RenamePair("FlowerPos", "flower_pos")
        }));
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:end_crystal", new RootPositionConverter(VERSION, new RenamePair[] {
                new RenamePair("BeamTarget", "beam_target"),
        }));
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:wandering_trader", new RootPositionConverter(VERSION, new RenamePair[] {
                new RenamePair("WanderTarget", "wander_target"),
        }));

        final RootPositionConverter patrolConverter = new RootPositionConverter(VERSION, new RenamePair[] {
                new RenamePair("PatrolTarget", "patrol_target"),
        });
        for (final String id : PATROLLING_MOBS) {
            MCTypeRegistry.ENTITY.addConverterForId(id, patrolConverter);
        }

        MCTypeRegistry.ENTITY.addStructureConverter(new RootPositionConverter(VERSION, new RenamePair[] {
                new RenamePair("Leash", "leash"),
        }));


        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:beehive", new RootPositionConverter(VERSION, new RenamePair[] {
                new RenamePair("FlowerPos", "flower_pos"),
        }));
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:end_gateway", new RootPositionConverter(VERSION, new RenamePair[] {
                new RenamePair("ExitPortal", "exit_portal"),
        }));

        MCTypeRegistry.SAVED_DATA_MAP_DATA.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> root, final long sourceVersion, final long toVersion) {
                final MapType<String> data = root.getMap("data");

                if (data == null) {
                    return null;
                }

                final ListType frames = data.getList("frames", ObjectType.MAP);
                if (frames != null) {
                    for (int i = 0, len = frames.size(); i < len; ++i) {
                        final MapType<String> frame = frames.getMap(i);

                        V3807.flattenBlockPos(frame, "Pos");

                        RenameHelper.renameSingle(frame, "Pos", "pos");
                        RenameHelper.renameSingle(frame, "Rotation", "rotation");
                        RenameHelper.renameSingle(frame, "EntityId", "entity_id");
                    }
                }

                final ListType banners = data.getList("banners", ObjectType.MAP);
                for (int i = 0, len = banners.size(); i < len; ++i) {
                    final MapType<String> banner = banners.getMap(i);

                    RenameHelper.renameSingle(banner, "Pos", "pos");
                    RenameHelper.renameSingle(banner, "Color", "color");
                    RenameHelper.renameSingle(banner, "Name", "name");
                }

                return null;
            }
        });

        MCTypeRegistry.ITEM_STACK.addConverterForId("minecraft:compass", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> tag = data.getMap("tag");

                if (tag == null) {
                    return null;
                }

                V3807.flattenBlockPos(tag, "LodestonePos");

                return null;
            }
        });
    }

    private V3813() {}
    
    private static record RenamePair(String from, String to) {}
}
