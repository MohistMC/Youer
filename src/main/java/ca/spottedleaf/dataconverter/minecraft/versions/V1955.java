package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import net.minecraft.util.Mth;

public final class V1955 {

    private static final int VERSION = MCVersions.V1_14_1_PRE1;

    private static final int[] LEVEL_XP_THRESHOLDS = new int[] {
            0,
            10,
            50,
            100,
            150
    };

    private static int getMinXpPerLevel(final int level) {
        return LEVEL_XP_THRESHOLDS[Mth.clamp(level - 1, 0, LEVEL_XP_THRESHOLDS.length - 1)];
    }

    private static void addLevel(final MapType data, final int level) {
        final MapType villagerData = data.getOrCreateMap("VillagerData");
        villagerData.setInt("level", level);
    }

    private static void addXpFromLevel(final MapType data, final int level) {
        data.setInt("Xp", getMinXpPerLevel(level));
    }

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:villager", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType villagerData = data.getMap("VillagerData");
                int level = villagerData == null ? 0 : villagerData.getInt("level");
                if (level == 0 || level == 1) {
                    // count recipes
                    final MapType offers = data.getMap("Offers");
                    final ListType recipes = offers == null ? null : offers.getList("Recipes", ObjectType.MAP);
                    final int recipeCount;
                    if (recipes != null) {
                        recipeCount = recipes.size();
                    } else {
                        recipeCount = 0;
                    }

                    level = Mth.clamp(recipeCount / 2, 1, 5);
                    if (level > 1) {
                        addLevel(data, level);
                    }
                }

                if (!data.hasKey("Xp", ObjectType.NUMBER)) {
                    addXpFromLevel(data, level);
                }

                return null;
            }
        });

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:zombie_villager", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final Number xp = data.getNumber("Xp");
                if (xp == null) {
                    final int level;
                    final MapType villagerData = data.getMap("VillagerData");
                    if (villagerData == null) {
                        level = 1;
                    } else {
                        level = villagerData.getInt("level", 1);
                    }

                    data.setInt("Xp", getMinXpPerLevel(level));
                }
                return null;
            }
        });
    }

    private V1955() {}
}
