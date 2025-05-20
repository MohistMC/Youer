package org.bukkit.craftbukkit.v1_21_R1.legacy;

import org.bukkit.craftbukkit.v1_21_R1.legacy.reroute.NotInBukkit;
import org.bukkit.event.entity.EntityCombustEvent;

public class MethodRerouting {

    @NotInBukkit
    public static int getDuration(EntityCombustEvent event) {
        return (int) event.getDuration();
    }
}
