package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.game_event.GameEventListenerWalker;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;

public final class V3083 {

    private static final int VERSION = MCVersions.V22W12A + 1;

    private static void registerMob(final String id) {
        V100.registerEquipment(VERSION, id);
    }

    public static void register() {
        registerMob("minecraft:allay");
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:allay", new DataWalkerItemLists("Inventory"));
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:allay", new GameEventListenerWalker());
    }

    private V3083() {}
}
