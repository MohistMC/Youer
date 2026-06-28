package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4306 {

    private static final int VERSION = MCVersions.V25W03A + 2;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:potion", new DataConverter<>(VERSION) {
            private static boolean matchItem(final MapType item, final String id) {
                if (item == null) {
                    return false;
                }

                return id.equals(item.getString("id"));
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.setString("id", matchItem(data.getMap("Item"), "minecraft:lingering_potion") ? "minecraft:lingering_potion" : "minecraft:splash_potion");

                return null;
            }
        });

        MCTypeRegistry.ENTITY.copyWalkers(VERSION, "minecraft:potion", "minecraft:splash_potion");
        MCTypeRegistry.ENTITY.copyWalkers(VERSION, "minecraft:potion", "minecraft:lingering_potion");
        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:potion", DataWalker.noOp());
    }

    private V4306() {}
}
