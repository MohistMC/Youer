package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;

public final class V3808 {

    private static final int VERSION = MCVersions.V24W04A + 2;

    public static void register() {
        class BodyArmorConverter extends DataConverter<MapType<String>, MapType<String>> {
            private final String path;
            private final boolean clearArmor;

            public BodyArmorConverter(final int toVersion, final String path, final boolean clearArmor) {
                this(toVersion, 0, path, clearArmor);
            }

            public BodyArmorConverter(final int toVersion, final int versionStep, final String path, final boolean clearArmor) {
                super(toVersion, versionStep);

                this.path = path;
                this.clearArmor = clearArmor;
            }

            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> prev = data.getMap(this.path);

                if (prev == null) {
                    return null;
                }

                data.remove(this.path);

                data.setMap("body_armor_item", prev);
                data.setFloat("body_armor_drop_chance", 2.0F);

                if (this.clearArmor) {
                    final ListType armor = data.getList("ArmorItems", ObjectType.MAP);
                    if (armor != null && armor.size() > 2) {
                        armor.setMap(2, data.getTypeUtil().createEmptyMap());
                    }

                    final ListType chances = data.getList("ArmorDropChances", ObjectType.FLOAT);
                    if (chances != null && chances.size() > 2) {
                        chances.setFloat(2, 0.085F);
                    }
                }

                return null;
            }
        }

        // Step 0
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:horse", new BodyArmorConverter(VERSION, "ArmorItem", true));

        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:horse", new DataWalkerItems("SaddleItem"));
        V100.registerEquipment(VERSION, "minecraft:horse");

        // Step 1
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:llama", new BodyArmorConverter(VERSION, 1, "DecorItem", false));

        MCTypeRegistry.ENTITY.addWalker(VERSION, 1, "minecraft:llama", new DataWalkerItemLists("Items"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, 1, "minecraft:llama", new DataWalkerItems("SaddleItem"));
        V100.registerEquipment(VERSION, 1, "minecraft:llama");

        // Step 2
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:trader_llama", new BodyArmorConverter(VERSION, 2, "DecorItem", false));
        MCTypeRegistry.ENTITY.addWalker(VERSION, 2, "minecraft:trader_llama", new DataWalkerItemLists("Items"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, 2, "minecraft:trader_llama", new DataWalkerItems("SaddleItem"));
        V100.registerEquipment(VERSION, 2, "minecraft:trader_llama");
    }

    private V3808() {}
}
