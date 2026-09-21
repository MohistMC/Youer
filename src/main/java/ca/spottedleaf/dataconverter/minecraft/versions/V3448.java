package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.util.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerListPaths;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;

public final class V3448 {

    private static final int VERSION = MCVersions.V23W14A + 3;

    public static void register() {
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:decorated_pot", new DataWalkerListPaths<>(MCTypeRegistry.ITEM_NAME, "sherds"));
        MCTypeRegistry.TILE_ENTITY.addWalker(VERSION, "minecraft:decorated_pot", new DataWalkerItems("item"));
        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:decorated_pot", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "shards", "sherds");
                return null;
            }
        });
    }

    private V3448() {}
}
