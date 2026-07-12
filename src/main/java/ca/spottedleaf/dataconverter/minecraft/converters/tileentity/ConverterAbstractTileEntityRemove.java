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

                // Paper start - call event on block entity removal
                final java.util.function.Supplier<net.kyori.adventure.key.Key> worldKeySupplier = com.google.common.base.Suppliers.memoize(() -> {
                    final ca.spottedleaf.dataconverter.types.MapType context = data.getMap(net.minecraft.util.datafix.fixes.ChunkHeightAndBiomeFix.DATAFIXER_CONTEXT_TAG);
                    if (context == null) return null;

                    final @org.intellij.lang.annotations.Subst("minecraft:overworld") String levelIdentifier = context.getString("level_identifier");
                    if (levelIdentifier == null) return null;

                    return net.kyori.adventure.key.Key.key(levelIdentifier);
                });
                // Paper end - call event on block entity removal

                for (int i = 0; i < blockEntities.size(); ++i) {
                    if (isType(blockEntities.getMap(i), type)) {
                        // Paper start - call event on block entity removal
                        final net.kyori.adventure.key.Key worldKey = worldKeySupplier.get();
                        if (worldKey != null) {
                            final MapType blockEntity = blockEntities.getMap(i);
                            final Number x = blockEntity.getNumber("x");
                            final Number y = blockEntity.getNumber("y");
                            final Number z = blockEntity.getNumber("z");
                            if (x != null && y != null && z != null) {
                                final org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer container = new org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer(
                                        net.minecraft.util.datafix.fixes.RemoveBlockEntityTagFix.DATA_TYPE_REGISTRY
                                );
                                if (blockEntity.getMap("PublicBukkitValues") instanceof final ca.spottedleaf.dataconverter.types.nbt.NBTMapType nbtMapType) {
                                    container.putAll(nbtMapType.getTag());
                                }
                                new io.papermc.paper.event.server.AsyncServerDataFixerRemoveBlockEntityEvent(
                                        worldKey,
                                        net.kyori.adventure.key.Key.key(type),
                                        io.papermc.paper.math.Position.block(x.intValue(), y.intValue(), z.intValue()),
                                        container
                                ).callEvent();
                            }
                        }
                        // Paper end - call event on block entity removal
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
