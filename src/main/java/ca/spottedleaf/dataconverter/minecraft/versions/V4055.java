package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.attributes.ConverterAbstractAttributesRename;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;

public final class V4055 {

    private static final int VERSION = MCVersions.V1_21_1 + 100;

    private static final Prefix[] PREFIXES_TO_REMOVE = new Prefix[] {
        new Prefix("generic."),
        new Prefix("horse."),
        new Prefix("player."),
        new Prefix("zombie.")
    };

    private static record Prefix(String raw, String namespaced) {
        public Prefix(final String raw) {
            this(raw, NamespaceUtil.correctNamespace(raw));
        }
    }

    public static void register() {
        ConverterAbstractAttributesRename.register(VERSION, (final String input) -> {
            final String namespacedInput = NamespaceUtil.correctNamespace(input);

            for (final Prefix prefix : PREFIXES_TO_REMOVE) {
                if (!namespacedInput.startsWith(prefix.namespaced())) {
                    continue;
                }

                return "minecraft:".concat(namespacedInput.substring(prefix.namespaced().length()));
            }

            return null;
        });
    }

    private V4055() {}
}
