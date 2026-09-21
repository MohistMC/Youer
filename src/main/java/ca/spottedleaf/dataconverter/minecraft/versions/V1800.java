package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.itemname.ConverterAbstractItemRename;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class V1800 {

    private static final int VERSION = MCVersions.V1_13_2 + 169;

    public static final Map<String, String> RENAMED_ITEM_IDS = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("minecraft:cactus_green", "minecraft:green_dye")
                    .put("minecraft:rose_red", "minecraft:red_dye")
                    .put("minecraft:dandelion_yellow", "minecraft:yellow_dye")
                    .build()
    );

    public static void register() {
        ConverterAbstractItemRename.register(VERSION, RENAMED_ITEM_IDS::get);

        //registerMob("minecraft:panda"); // simple as of 1.21.5
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:pillager", new DataWalkerItemLists("Inventory"));
    }

    private V1800() {}
}
