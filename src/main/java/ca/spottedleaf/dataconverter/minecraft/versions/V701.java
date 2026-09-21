package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;

public final class V701 {

    private static final int VERSION = MCVersions.V1_10_2 + 189;

    public static void register() {
        MCTypeRegistry.ENTITY.addConverterForId("Skeleton", new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final int type = data.getInt("SkeletonType");
                data.remove("SkeletonType");

                switch (type) {
                    case 1:
                        data.setString("id", "WitherSkeleton");
                        break;
                    case 2:
                        data.setString("id", "Stray");
                        break;
                }

                return null;
            }
        });

        //registerMob("WitherSkeleton"); // is now simple in 1.21.5
        //registerMob("Stray"); // is now simple in 1.21.5
    }

    private V701() {}
}
