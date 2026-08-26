package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.converter.types.TypeUtil;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.HelperSpawnEggNameV105;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V105 {

    private static final int VERSION = MCVersions.V15W32C + 1;

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addConverterForId("minecraft:spawn_egg", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final TypeUtil<?> typeUtil = data.getTypeUtil();
                MapType tag = data.getMap("tag");
                if (tag == null) {
                    tag = typeUtil.createEmptyMap();
                }

                final short damage = data.getShort("Damage");
                if (damage != 0) {
                    data.setShort("Damage", (short)0);
                }

                MapType entityTag = tag.getMap("EntityTag");
                if (entityTag == null) {
                    entityTag = typeUtil.createEmptyMap();
                }

                if (!entityTag.hasKey("id", ObjectType.STRING)) {
                    final String converted = HelperSpawnEggNameV105.getSpawnNameFromId(damage);
                    if (converted != null) {
                        entityTag.setString("id", converted);
                        tag.setMap("EntityTag", entityTag);
                        data.setMap("tag", tag);
                    }
                }

                return null;
            }
        });
    }

    private V105() {}
}
