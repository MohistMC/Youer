package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import java.util.HashMap;
import java.util.Map;

public final class V3800 {

    private static final int VERSION = MCVersions.V1_20_4 + 100;

    public static void register() {
        final Map<String, String> renames = new HashMap<>(
                Map.of(
                        "minecraft:scute", "minecraft:turtle_scute"
                )
        );

        ConverterAbstractItemRename.register(VERSION, renames::get);
    }

    private V3800() {}
}
