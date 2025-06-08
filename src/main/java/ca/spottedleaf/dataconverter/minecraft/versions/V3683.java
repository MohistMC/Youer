package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.DataWalkerTypePaths;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V3683 {

    private static final int VERSION = MCVersions.V23W41A + 2;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("minecraft:tnt", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "Fuse", "fuse");

                final MapType<String> defaultState = data.getTypeUtil().createEmptyMap();
                data.setMap("block_state", defaultState);

                defaultState.setString("Name", "minecraft:tnt");

                return null;
            }
        });

        MCTypeRegistry.ENTITY.addWalker(VERSION, "minecraft:tnt", new DataWalkerTypePaths<>(MCTypeRegistry.BLOCK_STATE, "block_state"));
    }

    private V3683() {}
}
