package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItemLists;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.Types;

public final class V135 {

    private static final int VERSION = MCVersions.V15W40B + 1;

    public static void register() {
        // In this update they changed the "Riding" value to be "Passengers", which is now a list. So it added
        // support for multiple entities riding. Of course, Riding and Passenger are opposites - so it also will
        // switch the data layout to be from highest rider to lowest rider, in terms of depth.
        MCTypeRegistry.ENTITY.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(MapType data, final long sourceVersion, final long toVersion) {
                MapType ret = null;
                while (data.hasKey("Riding", ObjectType.MAP)) {
                    final MapType riding = data.getMap("Riding");
                    data.remove("Riding");

                    final ListType passengers = Types.NBT.createEmptyList();
                    riding.setList("Passengers", passengers);
                    passengers.addMap(data);

                    ret = data = riding;
                }

                return ret;
            }
        });


        MCTypeRegistry.PLAYER.addStructureWalker(VERSION, new DataWalkerItemLists("Inventory", "EnderItems"));
        MCTypeRegistry.PLAYER.addStructureWalker(VERSION, (final MapType data, final long fromVersion, final long toVersion) -> {
            final MapType rootVehicle = data.getMap("RootVehicle");
            if (rootVehicle != null) {
                WalkerUtils.convert(MCTypeRegistry.ENTITY, rootVehicle, "Entity", fromVersion, toVersion);
            }

            WalkerUtils.convertList(MCTypeRegistry.ENTITY, data, "ender_pearls", fromVersion, toVersion);

            return null;
        });

        MCTypeRegistry.ENTITY.addStructureWalker(VERSION, (final MapType data, final long fromVersion, final long toVersion) -> {
            WalkerUtils.convertList(MCTypeRegistry.ENTITY, data, "Passengers", fromVersion, toVersion);

            return MCTypeRegistry.ENTITY_EQUIPMENT.convert(data, fromVersion, toVersion);
        });

    }

    private V135() {}
}
