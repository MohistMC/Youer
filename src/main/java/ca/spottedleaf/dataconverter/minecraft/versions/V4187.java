package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.attributes.ConverterEntityAttributesBaseValueUpdater;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V4187 {

    private static final int VERSION = MCVersions.V1_21_4_RC2 + 1;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId(
            "minecraft:villager",
            new ConverterEntityAttributesBaseValueUpdater(
                VERSION, "minecraft:follow_range",
                (final double curr) -> {
                    return curr == 48.0 ? 16.0 : curr;
                }
            )
        );
        MCTypeRegistry.ENTITY.addConverterForId(
            "minecraft:bee",
            new ConverterEntityAttributesBaseValueUpdater(
                VERSION, "minecraft:follow_range",
                (final double curr) -> {
                    return curr == 48.0 ? 16.0 : curr;
                }
            )
        );
        MCTypeRegistry.ENTITY.addConverterForId(
            "minecraft:allay",
            new ConverterEntityAttributesBaseValueUpdater(
                VERSION, "minecraft:follow_range",
                (final double curr) -> {
                    return curr == 48.0 ? 16.0 : curr;
                }
            )
        );
        MCTypeRegistry.ENTITY.addConverterForId(
            "minecraft:llama",
            new ConverterEntityAttributesBaseValueUpdater(
                VERSION, "minecraft:follow_range",
                (final double curr) -> {
                    return curr == 48.0 ? 16.0 : curr;
                }
            )
        );
        MCTypeRegistry.ENTITY.addConverterForId(
            "minecraft:piglin_brute",
            new ConverterEntityAttributesBaseValueUpdater(
                VERSION, "minecraft:follow_range",
                (final double curr) -> {
                    return curr == 16.0 ? 12.0 : curr;
                }
            )
        );
        MCTypeRegistry.ENTITY.addConverterForId(
            "minecraft:warden",
            new ConverterEntityAttributesBaseValueUpdater(
                VERSION, "minecraft:follow_range",
                (final double curr) -> {
                    return curr == 16.0 ? 24.0 : curr;
                }
            )
        );
    }

    private V4187() {}
}
