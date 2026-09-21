package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.itemstack.ConverterEnchantmentsRename;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import java.util.HashMap;
import java.util.Map;

public final class V3803 {

    private static final int VERSION = MCVersions.V23W51B + 1;

    public static void register() {
        final Map<String, String> renames = new HashMap<>(
                Map.of(
                        "minecraft:sweeping", "minecraft:sweeping_edge"
                )
        );

        MCTypeRegistry.ITEM_STACK.addStructureConverter(new ConverterEnchantmentsRename(VERSION, renames::get));
    }

    private V3803() {}
}
