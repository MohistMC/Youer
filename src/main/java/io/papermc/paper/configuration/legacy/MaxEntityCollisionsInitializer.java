package io.papermc.paper.configuration.legacy;

import com.mohistmc.org.spongepowered.configurate.ConfigurationNode;
import com.mohistmc.org.spongepowered.configurate.objectmapping.meta.NodeResolver;
import com.mohistmc.org.spongepowered.configurate.util.NamingSchemes;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spigotmc.SpigotWorldConfig;

public class MaxEntityCollisionsInitializer implements NodeResolver {

    private final String name;
    private final SpigotWorldConfig spigotConfig;

    public MaxEntityCollisionsInitializer(String name, SpigotWorldConfig spigotConfig) {
        this.name = name;
        this.spigotConfig = spigotConfig;
    }

    @Override
    public @Nullable ConfigurationNode resolve(ConfigurationNode parent) {
        final String key = NamingSchemes.LOWER_CASE_DASHED.coerce(this.name);
        final ConfigurationNode node = parent.node(key);
        final int old = this.spigotConfig.getInt("max-entity-collisions", -1, false);
        if (node.virtual() && old > -1) {
            node.raw(old);
        }
        return node;
    }
}
