package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V1929 {

    private static final int VERSION = MCVersions.V19W04B + 2;

    public static void register() {
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:wandering_trader", (final MapType<String> data, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, data, "Inventory", fromVersion, toVersion);

            WalkerUtils.convertList(MCTypeRegistry.VILLAGER_TRADE, data.getMap("Offers"), "Recipes", fromVersion, toVersion);

            return null;
        });
        V100.registerEquipment(VERSION, "minecraft:wandering_trader");

        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:trader_llama", (final MapType<String> data, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, data, "SaddleItem", fromVersion, toVersion);
            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, data, "DecorItem", fromVersion, toVersion);

            WalkerUtils.convertList(MCTypeRegistry.ITEM_STACK, data, "Items", fromVersion, toVersion);

            return null;
        });
        V100.registerEquipment(VERSION, "minecraft:trader_llama");
    }

    private V1929() {}
}
