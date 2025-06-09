package io.papermc.paper.plugin.provider.configuration.type;

import java.util.List;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import com.mohistmc.org.spongepowered.configurate.objectmapping.ConfigSerializable;

// Record components used for deserialization!!!!
@ConfigSerializable
public record PermissionConfiguration(
    PermissionDefault defaultPerm,
    List<Permission> permissions) {
}
