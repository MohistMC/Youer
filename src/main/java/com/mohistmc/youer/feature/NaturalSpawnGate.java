package com.mohistmc.youer.feature;

import com.mohistmc.youer.feature.world.WorldConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Per-world gate behind worlds.yml {@code naturalspawn}. When a world has natural spawning off this
 * blocks entities tagged with the NATURAL spawn reason — but the wandering trader, phantoms and the
 * warden also tag their spawns NATURAL, so the per-world whitelist keeps them alive.
 */
public final class NaturalSpawnGate {

    private static final Map<String, CacheEntry> cache = new HashMap<>();

    private NaturalSpawnGate() {
    }

    public static boolean check(Entity entity, String worldName) {
        if (WorldConfig.naturalSpawn(worldName)) {
            return false;
        }
        if (entity.spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return false;
        }
        return !exemptTypes(worldName).contains(entity.getType());
    }

    /** Resolved once per config change, keyed by world; a spawn costs a set lookup, not a registry lookup. */
    private static Set<EntityType<?>> exemptTypes(String worldName) {
        List<String> configured = WorldConfig.naturalSpawnWhitelist(worldName);
        CacheEntry entry = cache.get(worldName);
        if (entry == null || !entry.source.equals(configured)) {
            Set<EntityType<?>> resolved = new HashSet<>(configured.size());
            for (String raw : configured) {
                ResourceLocation key = parseId(raw);
                EntityType<?> type = key == null ? null : BuiltInRegistries.ENTITY_TYPE.get(key);
                if (type != null) {
                    resolved.add(type);
                }
            }
            entry = new CacheEntry(configured, resolved);
            cache.put(worldName, entry);
        }
        return entry.resolved;
    }

    private static final class CacheEntry {
        final List<String> source;
        final Set<EntityType<?>> resolved;
        CacheEntry(List<String> source, Set<EntityType<?>> resolved) {
            this.source = source;
            this.resolved = resolved;
        }
    }

    /** Blank and malformed entries are skipped; tryParse rejects "" and surrounding spaces. */
    static ResourceLocation parseId(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : ResourceLocation.tryParse(trimmed);
    }
}
