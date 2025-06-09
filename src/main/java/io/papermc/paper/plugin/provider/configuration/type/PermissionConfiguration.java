package io.papermc.paper.plugin.provider.configuration.type;

import com.mohistmc.org.spongepowered.configurate.objectmapping.ConfigSerializable;
import java.util.List;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

// Record components used for deserialization!!!!
@ConfigSerializable
public record PermissionConfiguration(
    PermissionDefault defaultPerm,
    List<Permission> permissions) {
}
