package com.mohistmc.youer.bukkit;

import org.bukkit.Material;

/** Plugin-side array/ordinal pattern, passed through the real Commodore transformer by the test. */
public final class MaterialCacheFixture {
    public static Material[] createCache() {
        return Material.values();
    }

    public static Material lookup(Material[] cache, Material material) {
        return cache[material.ordinal()];
    }
}
