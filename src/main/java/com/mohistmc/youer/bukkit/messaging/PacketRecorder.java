package com.mohistmc.youer.bukkit.messaging;

import com.mohistmc.youer.Youer;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public class PacketRecorder {
    private final Object2IntArrayMap<ResourceLocation> unknown = new Object2IntArrayMap<>();
    private long lastUpdate = Util.getMillis();

    public PacketRecorder() {
        unknown.defaultReturnValue(0);
    }

    public void recordUnknown(ResourceLocation id) {
        if (id == null) {
            Youer.LOGGER.debug("Received packet with null id. This should never happen.");
            return;
        }
        int num = unknown.getInt(id);
        unknown.put(id, num + 1);
    }

    public void update() {
        long now = Util.getMillis();
        if (Math.abs(now - lastUpdate) > 5 * 60 * 1000) {
            consumeAndLog();
            lastUpdate = now;
        }
    }

    public void consumeAndLog() {
        String unknowns = unknown.object2IntEntrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(it -> it.getKey().toString() + '(' + it.getIntValue() + ')')
                .collect(Collectors.joining(", ", "unknown=[", "];"));
        unknown.clear();

        Youer.LOGGER.debug("Packet error statistics: {}", unknowns);
    }
}
