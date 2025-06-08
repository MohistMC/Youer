package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.advancements.ConverterAbstractAdvancementsRename;
import ca.spottedleaf.dataconverter.minecraft.converters.recipe.ConverterAbstractRecipeRename;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class V2100 {

    private static final int VERSION = MCVersions.V1_14_4 + 124;
    private static final Map<String, String> RECIPE_RENAMES = new HashMap<>(
            ImmutableMap.of(
                    "minecraft:sugar", "minecraft:sugar_from_sugar_cane"
            )
    );
    private static final Map<String, String> ADVANCEMENT_RENAMES = new HashMap<>(
            ImmutableMap.of(
                    "minecraft:recipes/misc/sugar", "minecraft:recipes/misc/sugar_from_sugar_cane"
            )
    );

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        ConverterAbstractRecipeRename.register(VERSION, RECIPE_RENAMES::get);
        ConverterAbstractAdvancementsRename.register(VERSION, ADVANCEMENT_RENAMES::get);

        registerMob("minecraft:bee");
        registerMob("minecraft:bee_stinger");
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:beehive", (data, fromVersion, toVersion) -> {
            final ListType bees = data.getList("Bees", ObjectType.MAP);
            if (bees != null) {
                for (int i = 0, len = bees.size(); i < len; ++i) {
                    WalkerUtils.convert(MCTypeRegistry.ENTITY, bees.getMap(i), "EntityData", fromVersion, toVersion);
                }
            }

            return null;
        });
    }

    private V2100() {}
}
