package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5013 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT9 + 2;

    private static boolean isIntermediate(final @Nullable String status) {
        return "minecraft:noise".equals(status) || "minecraft:surface".equals(status);
    }

    private static String fixStatus(final String status) {
        if ("minecraft:carvers".equals(status)) {
            return "minecraft:terrain";
        }
        return isIntermediate(status) ? "minecraft:biomes" : status;
    }

    public static void register() {
        MCTypeRegistry.CHUNK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final String status = NamespaceUtil.correctNamespace(data.getString("Status", "minecraft:empty"));
                data.setString("Status", fixStatus(status));

                final MapType retrogen = data.getMap("below_zero_retrogen");
                final String targetStatus;
                if (retrogen != null) {
                    targetStatus = NamespaceUtil.correctNamespace(retrogen.getString("target_status", "minecraft:empty"));
                    retrogen.setString("target_status", fixStatus(targetStatus));
                } else {
                    targetStatus = null;
                }

                if (isIntermediate(status) || isIntermediate(targetStatus)) {
                    final boolean onlyBelowZero = targetStatus != null && !isIntermediate(targetStatus);
                    data.remove("Heightmaps");
                    data.remove("blending_data");
                    final ListType sections = data.getList("sections", ObjectType.MAP);
                    if (sections != null) {
                        for (int i = 0, len = sections.size(); i < len; ++i) {
                            final MapType section = sections.getMap(i);
                            if (!onlyBelowZero || section.getByte("Y") < 0) {
                                section.remove("block_states");
                            }
                        }
                    }
                }
                return null;
            }
        });
    }

    private V5013() {}
}
