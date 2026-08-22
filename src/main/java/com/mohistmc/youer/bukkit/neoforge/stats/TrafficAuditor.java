package com.mohistmc.youer.bukkit.neoforge.stats;

import com.mohistmc.youer.Youer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class TrafficAuditor {
    private final ConcurrentHashMap<Identifier, AtomicInteger> unknown = new ConcurrentHashMap<>();
    private long lastUpdate = Util.getMillis();

    public TrafficAuditor() {
    }

    public synchronized void note(Identifier id) {
        if (id == null) {
            Youer.LOGGER.debug("Received packet with null id. This should never happen.");
            return;
        }
        unknown.computeIfAbsent(id, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void tick() {
        long now = Util.getMillis();
        if (Math.abs(now - lastUpdate) > 5 * 60 * 1000) {
            flushAndLog();
            lastUpdate = now;
        }
    }

    public void flushAndLog() {
        ConcurrentHashMap<Identifier, AtomicInteger> snapshot = new ConcurrentHashMap<>(unknown);
        unknown.clear(); // clear original data

        String unknowns = snapshot.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> entry.getKey().toString() + '(' + entry.getValue().get() + ')')
                .collect(Collectors.joining(", ", "unknown=[", "];"));

        Youer.LOGGER.debug("Packet error statistics: {}", unknowns);
    }
}