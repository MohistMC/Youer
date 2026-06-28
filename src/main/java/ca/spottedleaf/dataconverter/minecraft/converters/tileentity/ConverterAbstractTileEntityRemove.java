package ca.spottedleaf.dataconverter.minecraft.converters.tileentity;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class ConverterAbstractTileEntityRemove {

    public static void register(final int version, final String type) {
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(version) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType components = data.getMap("components");
                if (components == null) {
                    return null;
                }

                if (isType(components.getMap("minecraft:block_entity_data"), type)) {
                    components.remove("minecraft:block_entity_data");
                }

                return null;
            }
        });

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:falling_block", new DataConverter<>(version) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (isType(data.getMap("TileEntityData"), type)) {
                    data.remove("TileEntityData");
                }

                return null;
            }
        });

        MCTypeRegistry.STRUCTURE.addStructureConverter(new DataConverter<>(version) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ca.spottedleaf.dataconverter.types.ListType blocks = data.getList("blocks", ca.spottedleaf.dataconverter.types.ObjectType.MAP);
                if (blocks == null) {
                    return null;
                }

                for (int i = 0, len = blocks.size(); i < len; ++i) {
                    final MapType block = blocks.getMap(i);
                    if (isType(block.getMap("nbt"), type)) {
                        block.remove("nbt");
                    }
                }

                return null;
            }
        });

        MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(version) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ca.spottedleaf.dataconverter.types.ListType blockEntities = data.getList("block_entities", ca.spottedleaf.dataconverter.types.ObjectType.MAP);
                if (blockEntities == null) {
                    return null;
                }

                for (int i = 0; i < blockEntities.size(); ++i) {
                    if (isType(blockEntities.getMap(i), type)) {
                        blockEntities.remove(i--);
                    }
                }

                return null;
            }
        });
    }

    private static boolean isType(final MapType data, final String type) {
        return data != null && type.equals(data.getString("id"));
    }
}
