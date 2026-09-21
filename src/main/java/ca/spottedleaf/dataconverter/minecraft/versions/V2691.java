package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.blockname.ConverterAbstractBlockRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class V2691 {

    private static final int VERSION = MCVersions.V21W05A + 1;

    private static final Map<String, String> RENAMES = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("minecraft:waxed_copper", "minecraft:waxed_copper_block")
                    .put("minecraft:oxidized_copper_block", "minecraft:oxidized_copper")
                    .put("minecraft:weathered_copper_block", "minecraft:weathered_copper")
                    .put("minecraft:exposed_copper_block", "minecraft:exposed_copper")
                    .build()
    );

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, RENAMES::get);
        ConverterAbstractBlockRename.register(VERSION, RENAMES::get);
    }

    private V2691() {}
}
