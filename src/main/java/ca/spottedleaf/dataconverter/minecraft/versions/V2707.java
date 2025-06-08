package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.AddFlagIfAbsent;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V2707 {

    private static final int VERSION = MCVersions.V21W14A + 1;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        MCTypeRegistry.WORLD_GEN_SETTINGS.addStructureConverter(new AddFlagIfAbsent(VERSION, "has_increased_height_already", true));

        registerMob("minecraft:marker"); // ?????????????
    }

    private V2707() {}
}
