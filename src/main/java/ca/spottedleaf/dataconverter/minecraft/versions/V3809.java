package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V3809 {

    private static final int VERSION = MCVersions.V24W05A;

    public static void register() {
        final DataConverter<MapType, MapType> slotConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final ListType items = data.getList("Items", ObjectType.MAP);
                if (items == null) {
                    return null;
                }

                for (int i = 0, len = items.size(); i < len; ++i) {
                    final MapType item = items.getMap(i);

                    final int slot = item.getInt("Slot", 2);
                    item.setByte("Slot", (byte)(slot - 2));
                }

                return null;
            }
        };

        MCTypeRegistry.ENTITY.addConverterForId("minecraft:llama", slotConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:trader_llama", slotConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:mule", slotConverter);
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:donkey", slotConverter);
    }

    private V3809() {}
}
