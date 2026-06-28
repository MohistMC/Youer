package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;

public final class V4175 {

    private static final int VERSION = MCVersions.V24W44A + 1;

    public static void register() {
        MCTypeRegistry.DATA_COMPONENTS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data.getMap("minecraft:equippable"), "model", "asset_id");

                final Number modelData = data.getNumber("minecraft:custom_model_data");
                if (modelData != null) {
                    final TypeUtil<?> typeUtil = data.getTypeUtil();

                    final MapType newModelData = typeUtil.createEmptyMap();
                    data.setMap("minecraft:custom_model_data", newModelData);

                    final ListType floats = typeUtil.createEmptyList();
                    newModelData.setList("floats", floats);

                    floats.addFloat(modelData.floatValue());
                }

                return null;
            }
        });
    }

    private V4175() {}
}
