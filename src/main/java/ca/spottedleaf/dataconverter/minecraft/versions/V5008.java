package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5008 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT6 + 3;

    private static final Map<String, String> MAP_ITEMS = Map.ofEntries(
        Map.entry("minecraft:mansion", "minecraft:woodland_explorer_map"),
        Map.entry("minecraft:monument", "minecraft:ocean_explorer_map"),
        Map.entry("minecraft:trial_chambers", "minecraft:trial_explorer_map"),
        Map.entry("minecraft:jungle_temple", "minecraft:jungle_explorer_map"),
        Map.entry("minecraft:swamp_hut", "minecraft:swamp_explorer_map"),
        Map.entry("minecraft:village_desert", "minecraft:desert_village_map"),
        Map.entry("minecraft:village_plains", "minecraft:plains_village_map"),
        Map.entry("minecraft:village_savanna", "minecraft:savanna_village_map"),
        Map.entry("minecraft:village_snowy", "minecraft:snowy_village_map"),
        Map.entry("minecraft:village_taiga", "minecraft:taiga_village_map"),
        Map.entry("minecraft:red_x", "minecraft:buried_treasure_map")
    );
    private static final Map<String, String> MAP_NAMES = Map.ofEntries(
        Map.entry("minecraft:mansion", "filled_map.mansion"),
        Map.entry("minecraft:monument", "filled_map.monument"),
        Map.entry("minecraft:trial_chambers", "filled_map.trial_chambers"),
        Map.entry("minecraft:jungle_temple", "filled_map.explorer_jungle"),
        Map.entry("minecraft:swamp_hut", "filled_map.explorer_swamp"),
        Map.entry("minecraft:village_desert", "filled_map.village_desert"),
        Map.entry("minecraft:village_plains", "filled_map.village_plains"),
        Map.entry("minecraft:village_savanna", "filled_map.village_savanna"),
        Map.entry("minecraft:village_snowy", "filled_map.village_snowy"),
        Map.entry("minecraft:village_taiga", "filled_map.village_taiga"),
        Map.entry("minecraft:red_x", "filled_map.buried_treasure")
    );

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addConverterForId("minecraft:filled_map", new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType components = data.getMap("components");
                if (components == null) {
                    return null;
                }
                final MapType decorations = components.getMap("minecraft:map_decorations");
                final MapType explorer = decorations == null ? null : decorations.getMap("+");
                if (explorer == null) {
                    return null;
                }
                final String type = NamespaceUtil.correctNamespace(explorer.getString("type", ""));
                final String item = MAP_ITEMS.get(type);
                if (item == null) {
                    return null;
                }
                data.setString("id", item);

                final MapType name = components.getMap("minecraft:item_name");
                if (name != null && name.size() == 1 && MAP_NAMES.get(type).equals(name.getString("translate"))) {
                    components.remove("minecraft:item_name");
                }
                return null;
            }
        });
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                data.remove("minecraft:map_color");
                return null;
            }
        });
    }

    private V5008() {}
}
