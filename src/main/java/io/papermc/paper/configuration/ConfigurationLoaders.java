package io.papermc.paper.configuration;

import com.mohistmc.org.spongepowered.configurate.loader.HeaderMode;
import com.mohistmc.org.spongepowered.configurate.util.MapFactories;
import com.mohistmc.org.spongepowered.configurate.yaml.NodeStyle;
import com.mohistmc.org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import java.nio.file.Path;

public final class ConfigurationLoaders {
    private ConfigurationLoaders() {
    }

    public static YamlConfigurationLoader.Builder naturallySorted() {
        return YamlConfigurationLoader.builder()
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .headerMode(HeaderMode.PRESET)
            .defaultOptions(options -> options.mapFactory(MapFactories.sortedNatural()));
    }

    public static YamlConfigurationLoader naturallySortedWithoutHeader(final Path path) {
        return naturallySorted()
            .headerMode(HeaderMode.NONE)
            .path(path)
            .build();
    }
}
