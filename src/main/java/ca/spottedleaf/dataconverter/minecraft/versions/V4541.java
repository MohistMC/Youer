package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.blockname.ConverterAbstractBlockRename;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import java.util.HashMap;
import java.util.Map;

public final class V4541 {

    private static final int VERSION = MCVersions.V25W34B + 1;

    private static final Map<String, String> CHAIN_RENAME = new HashMap<>(
        Map.of(
            "minecraft:chain", "minecraft:iron_chain"
        )
    );

    public static void register() {
        ConverterAbstractBlockRename.register(VERSION, CHAIN_RENAME::get);
        ConverterAbstractItemRename.register(VERSION, CHAIN_RENAME::get);
    }

    private V4541() {}
}
