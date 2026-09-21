package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.advancements.ConverterAbstractAdvancementsRename;
import java.util.HashMap;
import java.util.Map;

public final class V4311 {

    private static final int VERSION = MCVersions.V25W05A + 1;

    private static final Map<String, String> ADVANCEMENTS_RENAME = new HashMap<>();
    static {
        ADVANCEMENTS_RENAME.put("minecraft:nether/use_lodestone", "minecraft:adventure/use_lodestone");
    }

    public static void register() {
        ConverterAbstractAdvancementsRename.register(VERSION, ADVANCEMENTS_RENAME::get);
    }

    private V4311() {}
}
