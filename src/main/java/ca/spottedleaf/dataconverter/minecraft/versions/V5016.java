package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5016 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT10 + 1;

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addConverterForId("minecraft:map", new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType components = data.getMap("components");
                final MapType name = components == null ? null : components.getMap("minecraft:item_name");
                if (name != null && name.size() == 1 && "filled_map.buried_treasure".equals(name.getString("translate"))) {
                    name.setString("translate", "item.minecraft.buried_treasure_map");
                }
                return null;
            }
        });
    }

    private V5016() {}
}
