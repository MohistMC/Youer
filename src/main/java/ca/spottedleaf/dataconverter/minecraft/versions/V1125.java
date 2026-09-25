package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.hooks.DataHookValueTypeEnforceNamespaced;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.TypeUtil;

public final class V1125 {

    private static final int VERSION = MCVersions.V17W15A;
    private static final int BED_BLOCK_ID = 416;

    public static void register() {
        MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType level = data.getMap("Level");
                if (level == null) {
                    return null;
                }

                final int chunkX = level.getInt("xPos");
                final int chunkZ = level.getInt("zPos");

                final ListType sections = level.getList("Sections", ObjectType.MAP);
                if (sections == null) {
                    return null;
                }

                final ListType tileEntities = level.getOrCreateList("TileEntities", ObjectType.MAP);

                final TypeUtil<?> typeUtil = level.getTypeUtil();
                for (int i = 0, len = sections.size(); i < len; ++i) {
                    final MapType section = sections.getMap(i);

                    final byte sectionY = section.getByte("Y");
                    final byte[] blocks = section.getBytes("Blocks");

                    if (blocks == null) {
                        continue;
                    }

                    for (int blockIndex = 0; blockIndex < blocks.length; ++blockIndex) {
                        if (BED_BLOCK_ID != ((blocks[blockIndex] & 255) << 4)) {
                            continue;
                        }

                        final int localX = blockIndex & 15;
                        final int localZ = (blockIndex >> 4) & 15;
                        final int localY = (blockIndex >> 8) & 15;

                        final MapType newTile = typeUtil.createEmptyMap();
                        newTile.setString("id", "minecraft:bed");
                        newTile.setInt("x", localX + (chunkX << 4));
                        newTile.setInt("y", localY + (sectionY << 4));
                        newTile.setInt("z", localZ + (chunkZ << 4));
                        newTile.setShort("color", (short)14); // Red

                        tileEntities.addMap(newTile);
                    }
                }

                return null;
            }
        });

        MCTypeRegistry.ITEM_STACK.addConverterForId("minecraft:bed", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (data.getShort("Damage") == 0) {
                    data.setShort("Damage", (short)14); // Red
                }

                return null;
            }
        });


        MCTypeRegistry.ADVANCEMENTS.addStructureWalker(VERSION, new DataWalker<>() {
            @Override
            public MapType walk(final MapType data, final long fromVersion, final long toVersion) {
                WalkerUtils.convertKeys(MCTypeRegistry.BIOME, data.getMap("minecraft:adventure/adventuring_time"), "criteria", fromVersion, toVersion);
                WalkerUtils.convertKeys(MCTypeRegistry.ENTITY_NAME, data.getMap("minecraft:adventure/kill_a_mob"), "criteria", fromVersion, toVersion);
                WalkerUtils.convertKeys(MCTypeRegistry.ENTITY_NAME, data.getMap("minecraft:adventure/kill_all_mobs"), "criteria", fromVersion, toVersion);
                WalkerUtils.convertKeys(MCTypeRegistry.ENTITY_NAME, data.getMap("minecraft:adventure/bred_all_animals"), "criteria", fromVersion, toVersion);

                return null;
            }
        });

        // Enforce namespacing for ids
        MCTypeRegistry.BIOME.addStructureHook(VERSION, new DataHookValueTypeEnforceNamespaced());
    }

    private V1125() {}
}
