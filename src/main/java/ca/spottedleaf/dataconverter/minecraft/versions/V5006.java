package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.datatypes.DataWalker;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.converter.util.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class V5006 {

    private static final int VERSION = MCVersions.V26_3_SNAPSHOT6 + 1;

    public static void register() {
        MCTypeRegistry.BLOCK_STATE.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public @Nullable Object convert(final Object input, final long sourceVersion, final long toVersion) {
                if (!(input instanceof MapType data)) {
                    return null;
                }

                RenameHelper.renameSingle(data, "Name", "id");
                RenameHelper.renameSingle(data, "Properties", "properties");
                return null;
            }
        });

        // previous version: 1466
        MCTypeRegistry.STRUCTURE_FEATURE.addStructureWalker(VERSION, new DataWalker<>() {
            @Override
            public @Nullable MapType walk(final MapType data, final long fromVersion, final long toVersion) {
                final ListType list = data.getList("Children", ObjectType.MAP);
                if (list != null) {
                    for (int i = 0, len = list.size(); i < len; ++i) {
                        final MapType child = list.getMap(i);

                        WalkerUtils.convert(MCTypeRegistry.BLOCK_STATE, child, "CA", fromVersion, toVersion);
                        WalkerUtils.convert(MCTypeRegistry.BLOCK_STATE, child, "CB", fromVersion, toVersion);
                        WalkerUtils.convert(MCTypeRegistry.BLOCK_STATE, child, "CC", fromVersion, toVersion);
                        WalkerUtils.convert(MCTypeRegistry.BLOCK_STATE, child, "CD", fromVersion, toVersion);

                        walkPoolElement(child.getMap("pool_element"), fromVersion, toVersion);
                    }
                }

                WalkerUtils.convert(MCTypeRegistry.BIOME, data, "biome", fromVersion, toVersion);

                return null;
            }
        });
    }

    private static void walkPoolElement(final @Nullable MapType element, final long fromVersion, final long toVersion) {
        if (element == null) {
            return;
        }

        final ListType elements = element.getList("elements", ObjectType.MAP);
        if (elements != null) {
            for (int i = 0, len = elements.size(); i < len; ++i) {
                walkPoolElement(elements.getMap(i), fromVersion, toVersion);
            }
        }

        final Object processors = element.getGeneric("processors");
        if (processors instanceof ListType list) {
            walkProcessors(list, fromVersion, toVersion);
        } else if (processors instanceof MapType map) {
            walkProcessors(map.getList("processors", ObjectType.MAP), fromVersion, toVersion);
        }
    }

    private static void walkProcessors(final @Nullable ListType processors, final long fromVersion, final long toVersion) {
        if (processors == null) {
            return;
        }

        for (int i = 0, len = processors.size(); i < len; ++i) {
            walkProcessor(processors.getMap(i, null), fromVersion, toVersion);
        }
    }

    private static void walkProcessor(final @Nullable MapType processor, final long fromVersion, final long toVersion) {
        if (processor == null) {
            return;
        }

        switch (NamespaceUtil.correctNamespace(processor.getString("processor_type", ""))) {
            case "minecraft:rule": {
                final ListType rules = processor.getList("rules", ObjectType.MAP);
                if (rules == null) {
                    break;
                }

                for (int i = 0, len = rules.size(); i < len; ++i) {
                    final MapType rule = rules.getMap(i);

                    walkRuleTest(rule.getMap("input_predicate"), fromVersion, toVersion);
                    walkRuleTest(rule.getMap("location_predicate"), fromVersion, toVersion);
                    WalkerUtils.convert(MCTypeRegistry.BLOCK_STATE, rule, "output_state", fromVersion, toVersion);
                }
                break;
            }
            case "minecraft:block_ignore": {
                WalkerUtils.convertList(MCTypeRegistry.BLOCK_STATE, processor, "blocks", fromVersion, toVersion);
                break;
            }
            case "minecraft:capped": {
                walkProcessor(processor.getMap("delegate"), fromVersion, toVersion);
                break;
            }
            default: {
                break;
            }
        }
    }

    private static void walkRuleTest(final @Nullable MapType test, final long fromVersion, final long toVersion) {
        if (test == null) {
            return;
        }

        switch (NamespaceUtil.correctNamespace(test.getString("predicate_type", ""))) {
            case "minecraft:block_match":
            case "minecraft:random_block_match": {
                WalkerUtils.convert(MCTypeRegistry.BLOCK_NAME, test, "block", fromVersion, toVersion);
                break;
            }
            case "minecraft:blockstate_match":
            case "minecraft:random_blockstate_match": {
                WalkerUtils.convert(MCTypeRegistry.BLOCK_STATE, test, "block_state", fromVersion, toVersion);
                break;
            }
            default: {
                break;
            }
        }
    }

    private V5006() {}
}
