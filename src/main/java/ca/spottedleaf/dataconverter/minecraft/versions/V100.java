package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.block_name.DataWalkerBlockNames;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.Types;

public final class V100 {

    private static final int VERSION = MCVersions.V15W32A;

    public static void register() {
        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ListType equipment = data.getList("Equipment", ObjectType.MAP);
                data.remove("Equipment");

                if (equipment != null) {
                    if (equipment.size() > 0 && data.getListUnchecked("HandItems") == null) {
                        final ListType handItems = Types.NBT.createEmptyList();
                        data.setList("HandItems", handItems);
                        handItems.addMap(equipment.getMap(0));
                        handItems.addMap(Types.NBT.createEmptyMap());
                    }

                    if (equipment.size() > 1 && data.getListUnchecked("ArmorItems") == null) {
                        final ListType armorItems = Types.NBT.createEmptyList();
                        data.setList("ArmorItems", armorItems);
                        for (int i = 1; i < Math.min(equipment.size(), 5); ++i) {
                            armorItems.addMap(equipment.getMap(i));
                        }
                    }
                }

                final ListType dropChances = data.getList("DropChances", ObjectType.FLOAT);
                data.remove("DropChances");

                if (dropChances != null) {
                    if (data.getListUnchecked("HandDropChances") == null) {
                        final ListType handDropChances = Types.NBT.createEmptyList();
                        data.setList("HandDropChances", handDropChances);
                        if (0 < dropChances.size()) {
                            handDropChances.addFloat(dropChances.getFloat(0));
                        } else {
                            handDropChances.addFloat(0.0F);
                        }
                        handDropChances.addFloat(0.0F);
                    }

                    if (data.getListUnchecked("ArmorDropChances") == null) {
                        final ListType armorDropChances = Types.NBT.createEmptyList();
                        data.setList("ArmorDropChances", armorDropChances);
                        for (int i = 1; i < 5; ++i) {
                            if (i < dropChances.size()) {
                                armorDropChances.addFloat(dropChances.getFloat(i));
                            } else {
                                armorDropChances.addFloat(0.0F);
                            }
                        }
                    }
                }

                return null;
            }
        });
        MCTypeRegistry.ENTITY_EQUIPMENT.addStructureWalker(VERSION, (final MapType data, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, data, "ArmorItems", fromVersion, toVersion);
            WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, data, "HandItems", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, data, "body_armor_item", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, data, "saddle", fromVersion, toVersion);

            return null;
        });

        //registerMob("ArmorStand"); // changed to simple in 1.21.5
        //registerMob("Creeper"); // changed to simple in 1.21.5
        //registerMob("Skeleton"); // changed to simple in 1.21.5
        //registerMob("Spider"); // changed to simple in 1.21.5
        //registerMob("Giant"); // changed to simple in 1.21.5
        //registerMob("Zombie"); // changed to simple in 1.21.5
        //registerMob("Slime"); // changed to simple in 1.21.5
        //registerMob("Ghast"); // changed to simple in 1.21.5
        //registerMob("PigZombie"); // changed to simple in 1.21.5
        // Enderman is now identical to V99 due to moving equipment to base in 1.21.5
        //registerMob("CaveSpider"); // changed to simple in 1.21.5
        //registerMob("Silverfish"); // changed to simple in 1.21.5
        //registerMob("Blaze"); // changed to simple in 1.21.5
        //registerMob("LavaSlime"); // changed to simple in 1.21.5
        //registerMob("EnderDragon"); // changed to simple in 1.21.5
        //registerMob("WitherBoss"); // changed to simple in 1.21.5
        //registerMob("Bat"); // changed to simple in 1.21.5
        //registerMob("Witch"); // changed to simple in 1.21.5
        //registerMob("Endermite"); // changed to simple in 1.21.5
        //registerMob("Guardian"); // changed to simple in 1.21.5
        //registerMob("Pig"); // changed to simple in 1.21.5
        //registerMob("Sheep"); // changed to simple in 1.21.5
        //registerMob("Cow"); // changed to simple in 1.21.5
        //registerMob("Chicken"); // changed to simple in 1.21.5
        //registerMob("Squid"); // changed to simple in 1.21.5
        //registerMob("Wolf"); // changed to simple in 1.21.5
        //registerMob("MushroomCow"); // changed to simple in 1.21.5
        //registerMob("SnowMan"); // changed to simple in 1.21.5
        //registerMob("Ozelot"); // changed to simple in 1.21.5
        //registerMob("VillagerGolem"); // changed to simple in 1.21.5
        // EntityHorse is now identical to V99 due to moving equipment to base in 1.21.5
        //registerMob("Rabbit"); // changed to simple in 1.21.5
        // Villager is now identical to V99 due to moving equipment to base in 1.21.5
        //registerMob("Shulker"); // changed to simple in 1.21.5
        // AreaEffectCloud is now identical to V99 due to moving equipment to base in 1.21.5

        // Moved to V99 in 1.21.5
    }

    private V100() {}
}
