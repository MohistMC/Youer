package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class V4299 {

    private static final int VERSION = MCVersions.V25W02A + 1;

    public static void register() {
        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
            private static final String[] AXOLOTL_VARIANT_LOOKUP = new String[] {
                "lucy",
                "wild",
                "gold",
                "cyan",
                "blue",
            };

            private static final Int2ObjectOpenHashMap<String> FISH_PATTERN_LOOKUP = new Int2ObjectOpenHashMap<>();
            static {
                FISH_PATTERN_LOOKUP.defaultReturnValue("kob");
                FISH_PATTERN_LOOKUP.put(1,"flopper");
                FISH_PATTERN_LOOKUP.put(256,"sunstreak");
                FISH_PATTERN_LOOKUP.put(257,"stripey");
                FISH_PATTERN_LOOKUP.put(512,"snooper");
                FISH_PATTERN_LOOKUP.put(513,"glitter");
                FISH_PATTERN_LOOKUP.put(768,"dasher");
                FISH_PATTERN_LOOKUP.put(769,"blockfish");
                FISH_PATTERN_LOOKUP.put(1024,"brinely");
                FISH_PATTERN_LOOKUP.put(1025,"betty");
                FISH_PATTERN_LOOKUP.put(1280,"spotty");
                FISH_PATTERN_LOOKUP.put(1281,"clayfish");
            }

            private static String lookupAxolotl(final int index) {
                return index >= 0 && index < AXOLOTL_VARIANT_LOOKUP.length ? AXOLOTL_VARIANT_LOOKUP[index] : AXOLOTL_VARIANT_LOOKUP[0];
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                final MapType components = data.getMap("components");
                if (components == null) {
                    return null;
                }

                final String id = data.getString("id");
                switch (id) {
                    case "minecraft:axolotl_bucket": {
                        final MapType bucketEntityData = components.getMap("minecraft:bucket_entity_data");
                        if (bucketEntityData == null) {
                            break;
                        }

                        final Number variant = bucketEntityData.getNumber("Variant");
                        if (variant == null) {
                            break;
                        }

                        bucketEntityData.remove("Variant");
                        components.setString("minecraft:axolotl/variant", lookupAxolotl(variant.intValue()));

                        break;
                    }
                    case "minecraft:salmon_bucket": {
                        final MapType bucketEntityData = components.getMap("minecraft:bucket_entity_data");
                        if (bucketEntityData == null) {
                            break;
                        }

                        final Object type = bucketEntityData.getGeneric("type");
                        if (type == null) {
                            break;
                        }
                        bucketEntityData.remove("type");
                        components.setGeneric("minecraft:salmon/size", type);

                        break;
                    }
                    case "minecraft:tropical_fish_bucket": {
                        final MapType bucketEntityData = components.getMap("minecraft:bucket_entity_data");
                        if (bucketEntityData == null) {
                            break;
                        }

                        final Number variantBoxed = bucketEntityData.getNumber("BucketVariantTag");
                        if (variantBoxed == null) {
                            break;
                        }
                        bucketEntityData.remove("BucketVariantTag");

                        final int variant = variantBoxed.intValue();

                        final String fishPattern = FISH_PATTERN_LOOKUP.get(variant & 0xFFFF);
                        final String baseColour = V3818.getBannerColour((variant >>> 16) & 0xFF);
                        final String patternColour = V3818.getBannerColour((variant >>> 24) & 0xFF);

                        components.setString("minecraft:tropical_fish/pattern", fishPattern);
                        components.setString("minecraft:tropical_fish/base_color", baseColour);
                        components.setString("minecraft:tropical_fish/pattern_color", patternColour);

                        break;
                    }
                    case "minecraft:painting": {
                        final MapType entityData = components.getMap("minecraft:entity_data");

                        if (entityData == null) {
                            break;
                        }

                        if (!"minecraft:painting".equals(entityData.getString("id"))) {
                            break;
                        }

                        final Object variant = entityData.getGeneric("variant");
                        if (variant != null) {
                            entityData.remove("variant");
                            components.setGeneric("minecraft:painting/variant", variant);
                        }
                        if (entityData.size() == 1) {
                            components.remove("minecraft:entity_data");
                        }

                        break;
                    }
                    default: {
                        return null;
                    }
                }

                return null;
            }
        });
    }

    private V4299() {}
}
