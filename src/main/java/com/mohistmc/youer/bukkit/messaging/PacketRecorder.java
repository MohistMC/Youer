package com.mohistmc.youer.bukkit.messaging;

import com.mohistmc.youer.Youer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public class PacketRecorder {
    private final ConcurrentHashMap<ResourceLocation, AtomicInteger> unknown = new ConcurrentHashMap<>();
    private long lastUpdate = Util.getMillis();

    public PacketRecorder() {
    }

    public synchronized void recordUnknown(ResourceLocation id) {
        if (id == null) {
            Youer.LOGGER.debug("Received packet with null id. This should never happen.");
            return;
        }
        unknown.computeIfAbsent(id, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void update() {
        long now = Util.getMillis();
        if (Math.abs(now - lastUpdate) > 5 * 60 * 1000) {
            consumeAndLog();
            lastUpdate = now;
        }
    }

    public void consumeAndLog() {
        ConcurrentHashMap<ResourceLocation, AtomicInteger> snapshot = new ConcurrentHashMap<>(unknown);
        unknown.clear(); // 清空原始数据

        String unknowns = snapshot.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> entry.getKey().toString() + '(' + entry.getValue().get() + ')')
                .collect(Collectors.joining(", ", "unknown=[", "];"));

        Youer.LOGGER.debug("Packet error statistics: {}", unknowns);
    }
}
