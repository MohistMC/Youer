package io.papermc.paper.plugin.provider.configuration.type;

import com.mohistmc.org.spongepowered.configurate.objectmapping.ConfigSerializable;
import com.mohistmc.org.spongepowered.configurate.objectmapping.meta.Required;

@ConfigSerializable
public record LoadConfiguration(
    @Required String name,
    boolean bootstrap
) {
}
