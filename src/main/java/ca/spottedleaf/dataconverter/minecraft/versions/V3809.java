package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;

public final class V3809 {

    private static final int VERSION = MCVersions.V24W05A;

    public static void register() {
        final DataConverter<MapType<String>, MapType<String>> slotConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final ListType items = data.getList("Items", ObjectType.MAP);
                if (items == null) {
                    return null;
                }

                for (int i = 0, len = items.size(); i < len; ++i) {
                    final MapType<String> item = items.getMap(i);

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
