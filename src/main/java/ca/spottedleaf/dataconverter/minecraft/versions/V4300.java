package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.types.MapType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class V4300 {

    private static final int VERSION = MCVersions.V25W02A + 2;

    public static void register() {
        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            private static final Set<String> SADDLE_ITEM_ENTITIES = new HashSet<>(
                Arrays.asList(
                    "minecraft:horse",
                    "minecraft:skeleton_horse",
                    "minecraft:zombie_horse",
                    "minecraft:donkey",
                    "minecraft:mule",
                    "minecraft:camel",
                    "minecraft:llama",
                    "minecraft:trader_llama"
                )
            );
            private static final Set<String> SADDLE_FLAG_ENTITIES = new HashSet<>(
                Arrays.asList(
                    "minecraft:pig",
                    "minecraft:strider"
                )
            );

            private static void setGuaranteedSaddleDropChance(final MapType data) {
                data.getOrCreateMap("drop_chances").setFloat("saddle", 2.0f);
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String id = data.getString("id");

                if (SADDLE_ITEM_ENTITIES.contains(id)) {
                    if (!RenameHelper.renameSingle(data, "SaddleItem", "saddle")) {
                        return null;
                    }

                    setGuaranteedSaddleDropChance(data);

                    return null;
                } else if (SADDLE_FLAG_ENTITIES.contains(id)) {
                    final boolean saddle = data.getBoolean("Saddle");
                    data.remove("Saddle");

                    if (!saddle) {
                        return null;
                    }

                    final MapType saddleItem = data.getTypeUtil().createEmptyMap();
                    data.setMap("saddle", saddleItem);
                    saddleItem.setString("id", "minecraft:saddle");
                    saddleItem.setInt("count", 1);

                    setGuaranteedSaddleDropChance(data);

                    return null;
                }

                return null;
            }
        });

        // saddle item moved to `saddle`, which is now under ENTITY_EQUIPMENT
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:llama", new DataWalkerItemLists("Items"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:trader_llama", new DataWalkerItemLists("Items"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:donkey", new DataWalkerItemLists("Items"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:mule", new DataWalkerItemLists("Items"));

        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:horse", DataWalker.noOp());
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:skeleton_horse", DataWalker.noOp());
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:zombie_horse", DataWalker.noOp());
    }

    private V4300() {}
}
