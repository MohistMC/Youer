package org.bukkit.craftbukkit.v1_21_R1.legacy.reroute;

import org.bukkit.craftbukkit.v1_21_R1.util.ApiVersion;

public record RequirePluginVersionData(ApiVersion minInclusive, ApiVersion maxInclusive) {

    public static RequirePluginVersionData create(RequirePluginVersion requirePluginVersion) {
        if (!requirePluginVersion.value().isBlank()) {
            if (!requirePluginVersion.minInclusive().isBlank() || !requirePluginVersion.maxInclusive().isBlank()) {
                throw new RuntimeException("When setting value, min inclusive and max inclusive data is not allowed.");
            }

            return new RequirePluginVersionData(ApiVersion.getOrCreateVersion(requirePluginVersion.value()), ApiVersion.getOrCreateVersion(requirePluginVersion.value()));
        }

        ApiVersion minInclusive = null;
        ApiVersion maxInclusive = null;

        if (!requirePluginVersion.minInclusive().isBlank()) {
            minInclusive = ApiVersion.getOrCreateVersion(requirePluginVersion.minInclusive());
        }
        if (!requirePluginVersion.maxInclusive().isBlank()) {
            maxInclusive = ApiVersion.getOrCreateVersion(requirePluginVersion.maxInclusive());
        }

        return new RequirePluginVersionData(minInclusive, maxInclusive);
    }

    public boolean test(ApiVersion pluginVersion) {
        if (minInclusive != null && pluginVersion.isOlderThan(minInclusive)) {
            return false;
        }

        if (maxInclusive != null && pluginVersion.isNewerThan(maxInclusive)) {
            return false;
        }

        return true;
    }
}
